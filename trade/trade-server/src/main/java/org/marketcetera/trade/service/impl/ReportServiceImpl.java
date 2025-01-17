package org.marketcetera.trade.service.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.apache.commons.lang.Validate;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.joda.time.DateTime;
import org.marketcetera.admin.User;
import org.marketcetera.admin.service.AuthorizationService;
import org.marketcetera.admin.service.UserService;
import org.marketcetera.admin.user.PersistentUser;
import org.marketcetera.brokers.service.BrokerService;
import org.marketcetera.core.IDFactory;
import org.marketcetera.core.LongIDFactory;
import org.marketcetera.core.PlatformServices;
import org.marketcetera.core.position.PositionKey;
import org.marketcetera.core.position.PositionKeyFactory;
import org.marketcetera.event.HasFIXMessage;
import org.marketcetera.eventbus.EventBusService;
import org.marketcetera.fix.ActiveFixSession;
import org.marketcetera.fix.FixSession;
import org.marketcetera.fix.FixSessionListener;
import org.marketcetera.fix.IncomingMessage;
import org.marketcetera.fix.dao.IncomingMessageDao;
import org.marketcetera.fix.dao.PersistentIncomingMessage;
import org.marketcetera.fix.dao.QPersistentIncomingMessage;
import org.marketcetera.persist.CollectionPageResponse;
import org.marketcetera.quickfix.FIXMessageUtil;
import org.marketcetera.quickfix.FIXVersion;
import org.marketcetera.symbol.SymbolResolverService;
import org.marketcetera.trade.AverageFillPrice;
import org.marketcetera.trade.AverageFillPriceFactory;
import org.marketcetera.trade.BrokerID;
import org.marketcetera.trade.ConvertibleBond;
import org.marketcetera.trade.Currency;
import org.marketcetera.trade.Equity;
import org.marketcetera.trade.ExecutionReport;
import org.marketcetera.trade.ExecutionReportSummary;
import org.marketcetera.trade.ExecutionType;
import org.marketcetera.trade.Factory;
import org.marketcetera.trade.Future;
import org.marketcetera.trade.HasMutableReportID;
import org.marketcetera.trade.Instrument;
import org.marketcetera.trade.Option;
import org.marketcetera.trade.OptionType;
import org.marketcetera.trade.OrderCancelReject;
import org.marketcetera.trade.OrderID;
import org.marketcetera.trade.OrderStatus;
import org.marketcetera.trade.OrderSummary;
import org.marketcetera.trade.Report;
import org.marketcetera.trade.ReportBase;
import org.marketcetera.trade.ReportBaseImpl;
import org.marketcetera.trade.ReportID;
import org.marketcetera.trade.ReportType;
import org.marketcetera.trade.RootOrderIdFactory;
import org.marketcetera.trade.SecurityType;
import org.marketcetera.trade.TradeMessage;
import org.marketcetera.trade.TradePermissions;
import org.marketcetera.trade.UserID;
import org.marketcetera.trade.dao.AverageFillQueryResult;
import org.marketcetera.trade.dao.ExecutionReportDao;
import org.marketcetera.trade.dao.PersistentExecutionReport;
import org.marketcetera.trade.dao.PersistentOrderSummary;
import org.marketcetera.trade.dao.PersistentReport;
import org.marketcetera.trade.dao.PersistentReportDao;
import org.marketcetera.trade.dao.QPersistentExecutionReport;
import org.marketcetera.trade.dao.QPersistentOrderSummary;
import org.marketcetera.trade.dao.QPersistentReport;
import org.marketcetera.trade.event.SimpleInjectedFixMessageEvent;
import org.marketcetera.trade.event.SimpleOrderSummaryEvent;
import org.marketcetera.trade.service.OrderSummaryService;
import org.marketcetera.trade.service.ReportService;
import org.marketcetera.util.log.SLF4JLoggerProxy;
import org.marketcetera.util.misc.ClassVersion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;

import quickfix.InvalidMessage;
import quickfix.SessionID;

/* $License$ */

/**
 * Provides access to report objects.
 *
 * @author <a href="mailto:colin@marketcetera.com">Colin DuPlantis</a>
 * @version $Id$
 * @since 2.4.2
 */
@Service
@Transactional(readOnly=true,propagation=Propagation.REQUIRED)
@ClassVersion("$Id$")
public class ReportServiceImpl
        implements ReportService,FixSessionListener
{
    /**
     * Validate and start the object.
     */
    @PostConstruct
    public void start()
    {
        reportIDFactory = new LongIDFactory(idFactory);
        timerService = new Timer();
        cachedSessionStart = CacheBuilder.newBuilder().build();
        Validate.isTrue(missingSeqNumBatchSize > 0,
                        "missingSeqNumBatchSize must be positive");
        SLF4JLoggerProxy.info(this,
                              "Report service started");
    }
    /**
     * Stop the object.
     */
    @PreDestroy
    public void stop()
    {
        if(timerService != null) {
            try {
                timerService.cancel();
            } catch (Exception ignored) {}
            timerService = null;
        }
    }
    /* (non-Javadoc)
     * @see org.marketcetera.trade.service.ReportService#addReport(org.marketcetera.event.HasFIXMessage, org.marketcetera.trade.BrokerID, org.marketcetera.trade.UserID)
     */
    @Override
    @Transactional(readOnly=false,propagation=Propagation.REQUIRED)
    public void addReport(HasFIXMessage inMessage,
                          BrokerID inBrokerId,
                          UserID inUserId)
    {
        quickfix.Message fixMessage = inMessage.getMessage();
        ActiveFixSession broker = brokerService.getActiveFixSession(inBrokerId);
        if(broker == null) {
            SLF4JLoggerProxy.warn(this,
                                  "Unable to set session ID on {} because the broker {} does not exist",
                                  fixMessage,
                                  inBrokerId);
        } else {
            SessionID sessionIdTarget = new SessionID(broker.getFixSession().getSessionId());
            // set the session ID as it would be set as if it came from the given broker
            FIXMessageUtil.setSessionId(fixMessage,
                                        FIXMessageUtil.getReversedSessionId(sessionIdTarget));
            FIXVersion fixVersion = FIXVersion.getFIXVersion(sessionIdTarget);
            fixVersion.getMessageFactory().addSendingTime(fixMessage);
        }
        if(!fixMessage.getHeader().isSetField(quickfix.field.MsgSeqNum.FIELD)) {
            fixMessage.getHeader().setField(new quickfix.field.MsgSeqNum(Integer.MIN_VALUE));
        }
        SLF4JLoggerProxy.info(this,
                              "Injecting: {}",
                              fixMessage);
        // set owner of this message
        User owner = userService.findByUserId(inUserId);
        if(owner == null) {
            SLF4JLoggerProxy.warn(this,
                                  "Unable to establish {} as the owner of {} because that user ID cannot be found. Normal methods will be used to establish the owner of this message.",
                                  inUserId,
                                  fixMessage);
        }
        eventBusService.post(new SimpleInjectedFixMessageEvent(owner,
                                                               inBrokerId,
                                                               fixMessage));
    }
    /* (non-Javadoc)
     * @see org.marketcetera.trade.service.ReportService#getReports(org.marketcetera.persist.PageRequest)
     */
    @Override
    public CollectionPageResponse<Report> getReports(org.marketcetera.persist.PageRequest inPageRequest)
    {
        Sort sort = buildSort(inPageRequest,
                              persistentReportAliases,
                              Sort.by(new Sort.Order(Sort.Direction.DESC,
                                                     QPersistentReport.persistentReport.sendingTime.getMetadata().getName()),
                                      new Sort.Order(Sort.Direction.DESC,
                                                     QPersistentReport.persistentReport.msgSeqNum.getMetadata().getName())));
        SLF4JLoggerProxy.debug(this,
                               "getReports sort order is {} renders: {}",
                               inPageRequest.getSortOrder(),
                               sort);
        Pageable pageRequest = PageRequest.of(inPageRequest.getPageNumber(),
                                              inPageRequest.getPageSize(),
                                              sort);
        Page<Report> pageResponse = persistentReportDao.findAllReports(pageRequest);
        return new CollectionPageResponse<>(pageResponse);
    }
    /* (non-Javadoc)
     * @see org.marketcetera.trade.service.ReportService#getFills(org.marketcetera.persist.PageRequest)
     */
    @Override
    public CollectionPageResponse<ExecutionReportSummary> getFills(org.marketcetera.persist.PageRequest inPageRequest)
    {
        Sort sort = buildSort(inPageRequest,
                              persistentExecutionReportAliases,
                              Sort.by(new Sort.Order(Sort.Direction.DESC,
                                                     QPersistentExecutionReport.persistentExecutionReport.sendingTime.getMetadata().getName()),
                                      new Sort.Order(Sort.Direction.DESC,
                                                     "report.msgSeqNum")));
        SLF4JLoggerProxy.debug(this,
                               "getFills sort order is {} renders: {}",
                               inPageRequest.getSortOrder(),
                               sort);
        Pageable pageRequest = PageRequest.of(inPageRequest.getPageNumber(),
                                              inPageRequest.getPageSize(),
                                              sort);
        Page<ExecutionReportSummary> executionReportPage = executionReportDao.findAllFills(pageRequest);
        CollectionPageResponse<ExecutionReportSummary> response = new CollectionPageResponse<>(executionReportPage);
        SLF4JLoggerProxy.debug(this,
                               "getFills returning {}",
                               response);
        return response;
    }
    /* (non-Javadoc)
     * @see com.marketcetera.ors.dao.ReportService#getReportFor(org.marketcetera.trade.ReportID)
     */
    @Override
    public PersistentReport getReportFor(ReportID inReportId)
    {
        return persistentReportDao.findByReportID(inReportId);
    }
    /* (non-Javadoc)
     * @see com.marketcetera.ors.dao.ReportService#save(com.marketcetera.ors.history.PersistentReport)
     */
    @Override
    @Transactional(readOnly=false,propagation=Propagation.REQUIRED)
    public PersistentReport save(Report inReport)
    {
        return persistentReportDao.save((PersistentReport)inReport);
    }
    /* (non-Javadoc)
     * @see com.marketcetera.ors.dao.ReportService#findLastSequenceNumberFor(quickfix.SessionID, java.util.Date)
     */
    @Override
    public int findLastSequenceNumberFor(SessionID inSessionId,
                                         Date inDate)
    {
        BooleanBuilder where = new BooleanBuilder();
        where = where.and(QPersistentIncomingMessage.persistentIncomingMessage.sessionId.eq(inSessionId.toString()));
        where = where.and(QPersistentIncomingMessage.persistentIncomingMessage.sendingTime.goe(inDate));
        Sort sort = Sort.by(new Sort.Order(Sort.Direction.DESC,
                                           QPersistentIncomingMessage.persistentIncomingMessage.sendingTime.getMetadata().getName()),
                            new Sort.Order(Sort.Direction.DESC,
                                           QPersistentIncomingMessage.persistentIncomingMessage.msgSeqNum.getMetadata().getName()));
        Pageable pageRequest = PageRequest.of(0,
                                              1,
                                              sort);
        SLF4JLoggerProxy.debug(this,
                               "Finding last seq nums for {} using {}",
                               inSessionId,
                               where);
        Page<PersistentIncomingMessage> mostRecentMessagePage = incomingMessageDao.findAll(where,
                                                                                           pageRequest);
        if(mostRecentMessagePage == null || !mostRecentMessagePage.hasContent()) {
            SLF4JLoggerProxy.debug(this,
                                   "No incoming fix messages for {}",
                                   inSessionId);
            return -1;
        }
        return mostRecentMessagePage.getContent().get(0).getMsgSeqNum();
    }
    /* (non-Javadoc)
     * @see com.marketcetera.ors.dao.ReportService#findUnhandledIncomingMessages(quickfix.SessionID, java.util.Set, java.util.Date)
     */
    @Override
    public List<Long> findUnhandledIncomingMessageIds(SessionID inSessionId,
                                                      Set<String> inMessageTypes,
                                                      Date inSince)
    {
        // this method searches for reports that have been received that have not yet been converted into executions
        if(SLF4JLoggerProxy.isDebugEnabled(this)) {
            SLF4JLoggerProxy.debug(this,
                                   "Searching for unhandled incoming messages for {} before {}",
                                   inSessionId,
                                   new DateTime(inSince.getTime()));
        }
        return persistentReportDao.findUnhandledIncomingMessageIds(inSessionId.toString(),
                                                                  inMessageTypes,
                                                                  inSince);
    }
    /* (non-Javadoc)
     * @see com.marketcetera.ors.dao.ReportService#findIncomingMessagesForIdIn(java.util.Set)
     */
    @Override
    public List<IncomingMessage> findIncomingMessagesForIdIn(Set<Long> inIds)
    {
        List<IncomingMessage> results = new ArrayList<>();
        if(inIds == null || inIds.isEmpty()) {
            return results;
        }
        Sort sort = Sort.by(new Sort.Order(Sort.Direction.ASC,
                                           QPersistentIncomingMessage.persistentIncomingMessage.sendingTime.getMetadata().getName()),
                            new Sort.Order(Sort.Direction.ASC,
                                           QPersistentIncomingMessage.persistentIncomingMessage.msgSeqNum.getMetadata().getName()));
        Pageable pageRequest = PageRequest.of(0,
                                              Integer.MAX_VALUE,
                                              sort);
        for(PersistentIncomingMessage incomingMessage : incomingMessageDao.findByIdIn(inIds,
                                                                                      pageRequest)) {
            results.add(incomingMessage);
        }
        return results;
    }
    /* (non-Javadoc)
     * @see com.marketcetera.ors.dao.ExecutionReportService#purgeReportsBefore(java.util.Date)
     */
    @Override
    @Transactional(readOnly=false,propagation=Propagation.REQUIRED)
    public int purgeReportsBefore(Date inPurgeDate)
    {
        List<PersistentReport> reports = persistentReportDao.findBySendingTimeBefore(inPurgeDate);
        if(reports == null || reports.isEmpty()) {
            return 0;
        }
        List<Long> ids = new ArrayList<Long>();
        for(PersistentReport report : reports) {
            ids.add(report.getId());
        }
        // delete report summaries first - need to use a manual query here to include the list param
        entityManager.createNativeQuery("DELETE FROM exec_reports WHERE report_id IN (:ids)").setParameter("ids",ids).executeUpdate();
        // now, delete the reports
        persistentReportDao.deleteAll(reports);
        return reports.size();
    }
    /* (non-Javadoc)
     * @see com.marketcetera.ors.dao.ReportService#getOrderStatusForOrderChain(org.marketcetera.trade.OrderID)
     */
    @Override
    public OrderStatus getOrderStatusForOrderChain(OrderID inOrderId)
    {
        SLF4JLoggerProxy.debug(this,
                               "Searching for order status for {}",
                               inOrderId);
        OrderID rootId = executionReportDao.findRootIDForOrderID(inOrderId);
        SLF4JLoggerProxy.debug(this,
                               "Root order id for {} is {}",
                               inOrderId,
                               rootId);
        if(rootId == null) {
            return OrderStatus.Unknown;
        }
        BooleanBuilder where = new BooleanBuilder().and(QPersistentExecutionReport.persistentExecutionReport.rootOrderId.eq(rootId));
        Sort sort = Sort.by(Sort.Direction.DESC,
                            QPersistentExecutionReport.persistentExecutionReport.sendingTime.getMetadata().getName());
        PageRequest page = PageRequest.of(0,
                                          1,
                                          sort);
        SLF4JLoggerProxy.debug(this,
                               "Searching for status values for {}",
                               rootId);
        Page<PersistentExecutionReport> statusValues = executionReportDao.findAll(where,
                                                                                  page);
        OrderStatus orderStatus = OrderStatus.Unknown;
        if(statusValues.hasContent()) {
            orderStatus = statusValues.getContent().iterator().next().getOrderStatus();
        }
        SLF4JLoggerProxy.debug(this,
                               "Retrieved status value {} for {}",
                               orderStatus,
                               rootId);
        return orderStatus;
    }
    /* (non-Javadoc)
     * @see org.marketcetera.trade.service.ReportService#getExecutionReport(org.marketcetera.trade.OrderID, org.marketcetera.trade.OrderID)
     */
    @Override
    public Optional<ExecutionReport> getExecutionReport(OrderID inRootOrderId,
                                                        OrderID inOrderId)
    {
        SLF4JLoggerProxy.debug(this,
                               "Searching for the execution report for {}/{}",
                               inRootOrderId,
                               inOrderId);
        BooleanBuilder where = new BooleanBuilder().and(QPersistentExecutionReport.persistentExecutionReport.rootOrderId.eq(inRootOrderId));
        where = new BooleanBuilder().and(QPersistentExecutionReport.persistentExecutionReport.orderId.eq(inOrderId));
        Sort sort = Sort.by(Sort.Direction.DESC,
                            QPersistentExecutionReport.persistentExecutionReport.sendingTime.getMetadata().getName());
        PageRequest page = PageRequest.of(0,
                                          1,
                                          sort);
        Page<PersistentExecutionReport> executionReportPage = executionReportDao.findAll(where,
                                                                                         page);
        PersistentExecutionReport pExecutionReport = null;
        if(executionReportPage.hasContent()) {
            pExecutionReport = executionReportPage.getContent().iterator().next();
        }
        SLF4JLoggerProxy.debug(this,
                               "Retrieved {} for {}/{}",
                               pExecutionReport,
                               inRootOrderId,
                               inOrderId);
        ExecutionReport executionReport = null;
        if(pExecutionReport != null) {
            quickfix.Message fixMessage;
            try {
                fixMessage = new quickfix.Message(pExecutionReport.getReport().getFixMessage());
            } catch (InvalidMessage e) {
                SLF4JLoggerProxy.warn(this,
                                      "Cannot construct a FIX message from {}: {}",
                                      pExecutionReport.getReport().getFixMessage(),
                                      PlatformServices.getMessage(e));
                return Optional.empty();
            }
            executionReport = Factory.getInstance().createExecutionReport(fixMessage,
                                                                          pExecutionReport.getReport().getBrokerID(),
                                                                          pExecutionReport.getReport().getOriginator(),
                                                                          pExecutionReport.getActor()==null?null:pExecutionReport.getActor().getUserID(),
                                                                          pExecutionReport.getViewerID());
        }
        return Optional.ofNullable(executionReport);
    }
    /* (non-Javadoc)
     * @see org.marketcetera.trade.service.ReportService#getLatestExecutionReportForOrderChain(org.marketcetera.trade.OrderID)
     */
    @Override
    public Optional<ExecutionReport> getLatestExecutionReportForOrderChain(OrderID inOrderId)
    {
        SLF4JLoggerProxy.debug(this,
                               "Searching for the latest execution report for {}",
                               inOrderId);
        OrderID rootId = executionReportDao.findRootIDForOrderID(inOrderId);
        SLF4JLoggerProxy.debug(this,
                               "Root order id for {} is {}",
                               inOrderId,
                               rootId);
        if(rootId == null) {
            return Optional.empty();
        }
        BooleanBuilder where = new BooleanBuilder().and(QPersistentExecutionReport.persistentExecutionReport.rootOrderId.eq(rootId));
        Sort sort = Sort.by(Sort.Direction.DESC,
                            QPersistentExecutionReport.persistentExecutionReport.sendingTime.getMetadata().getName());
        PageRequest page = PageRequest.of(0,
                                          1,
                                          sort);
        SLF4JLoggerProxy.debug(this,
                               "Searching for an execution report for {}",
                               rootId);
        Page<PersistentExecutionReport> executionReportPage = executionReportDao.findAll(where,
                                                                                         page);
        PersistentExecutionReport pExecutionReport = null;
        if(executionReportPage.hasContent()) {
            pExecutionReport = executionReportPage.getContent().iterator().next();
        }
        SLF4JLoggerProxy.debug(this,
                               "Retrieved {} for {}",
                               pExecutionReport,
                               rootId);
        ExecutionReport executionReport = null;
        if(pExecutionReport != null) {
            quickfix.Message fixMessage;
            try {
                fixMessage = new quickfix.Message(pExecutionReport.getReport().getFixMessage());
            } catch (InvalidMessage e) {
                SLF4JLoggerProxy.warn(this,
                                      "Cannot construct a FIX message from {}: {}",
                                      pExecutionReport.getReport().getFixMessage(),
                                      PlatformServices.getMessage(e));
                return Optional.empty();
            }
            executionReport = Factory.getInstance().createExecutionReport(fixMessage,
                                                                          pExecutionReport.getReport().getBrokerID(),
                                                                          pExecutionReport.getReport().getOriginator(),
                                                                          pExecutionReport.getActor()==null?null:pExecutionReport.getActor().getUserID(),
                                                                          pExecutionReport.getViewerID());
        }
        return Optional.ofNullable(executionReport);
    }
    /* (non-Javadoc)
     * @see com.marketcetera.ors.dao.ReportService#getReportsSince(com.marketcetera.ors.security.SimpleUser, java.util.Date)
     */
    @Override
    public List<ReportBase> getReportsSince(User inUser,
                                            Date inDate)
    {
        QPersistentReport r = QPersistentReport.persistentReport;
        BooleanExpression where = r.sendingTime.goe(inDate);
        Set<User> basicUsers = authzService.getSubjectUsersFor(inUser,
                                                               TradePermissions.ViewReportAction.name());
        Set<PersistentUser> subjectUsers = Sets.newHashSet();
        for(User basicUser : basicUsers) {
            subjectUsers.add((PersistentUser)basicUser);
        }
        // show the report if the given user is this user or has supervisor permission over the report (the report permission has already been checked for the "same user" case)
        if(!subjectUsers.isEmpty()) {
            where = where.and(r.viewer.in(subjectUsers)).or(r.viewer.eq((PersistentUser)inUser));
        } else {
            where = where.and(r.viewer.eq((PersistentUser)inUser));
        }
        Sort sort = Sort.by(Sort.Direction.ASC,
                            r.sendingTime.getMetadata().getName());
        // can expose the page and page size to allow paging through the api interfaces
        PageRequest page = PageRequest.of(0,
                                          Integer.MAX_VALUE,
                                          sort);
        Iterable<PersistentReport> reports = persistentReportDao.findAll(where,
                                                                         page);
        List<ReportBase> results = new ArrayList<ReportBase>();
        for(PersistentReport report : reports) {
            results.add(report.toReport());
        }
        return results;
    }
    /* (non-Javadoc)
     * @see com.marketcetera.ors.dao.ReportService#getExecutions(int, int)
     */
    @Override
    public Page<? extends ExecutionReportSummary> getExecutions(int inPageNumber,
                                                                int inPageSize)
    {
        QPersistentExecutionReport e = QPersistentExecutionReport.persistentExecutionReport;
        Sort sort = Sort.by(Sort.Direction.ASC,
                            e.sendingTime.getMetadata().getName());
        PageRequest page = PageRequest.of(inPageNumber,
                                          inPageSize,
                                          sort);
        return executionReportDao.findAll(page);
    }
    /* (non-Javadoc)
     * @see org.marketcetera.trade.service.ReportService#getAverageFillPrice(org.marketcetera.persist.PageRequest)
     */
    @Override
    public CollectionPageResponse<AverageFillPrice> getAverageFillPrices(org.marketcetera.persist.PageRequest inPageRequest)
    {
        Sort sort = buildSort(inPageRequest,
                              persistentExecutionReportAliases,
                              Sort.by(new Sort.Order(Sort.Direction.ASC,
                                                     QPersistentExecutionReport.persistentExecutionReport.symbol.getMetadata().getName())));
        SLF4JLoggerProxy.debug(this,
                               "getAverageFillPrice sort order is {} renders: {}",
                               inPageRequest.getSortOrder(),
                               sort);
        Pageable pageRequest = PageRequest.of(inPageRequest.getPageNumber(),
                                              inPageRequest.getPageSize(),
                                              sort);
        Page<AverageFillQueryResult> result = executionReportDao.findAverageFillPrice(ExecutionType.FILLS,
                                                                                      pageRequest);
        CollectionPageResponse<AverageFillPrice> response = new CollectionPageResponse<>();
        response.setPageAttributes(result);
        for(AverageFillQueryResult queryResult : result.getContent()) {
            Instrument instrument = symbolResolverService.resolveSymbol(queryResult.getSymbol());
            if(instrument == null) {
                SLF4JLoggerProxy.warn(this,
                                      "Could not resolve an instrument from {}, skipping average fill result",
                                      queryResult);
                // this will mess up the paging, but, it really should be fixed by the engagement-specific symbol resolver service
                //  so, we won't worry about fixing the paging results
                continue;
            }
            response.getElements().add(averageFillPriceFactory.create(instrument,
                                                                      queryResult.getSide(),
                                                                      queryResult.getCumulativeQuantity(),
                                                                      queryResult.getWeightedAveragePrice()));
        }
        SLF4JLoggerProxy.debug(this,
                               "getAverageFillPrice: {}",
                               response);
        return response;
    }
    /* (non-Javadoc)
     * @see org.marketcetera.trade.service.ReportService#getPositionAsOf(org.marketcetera.admin.User, java.util.Date, org.marketcetera.trade.Instrument)
     */
    @Override
    public BigDecimal getPositionAsOf(User inUser,
                                      Date inDate,
                                      Instrument inInstrument)
    {
        switch(inInstrument.getSecurityType()) {
            case Currency:
                return getCurrencyPositionAsOf(inUser,
                                               inDate,
                                               (Currency)inInstrument);
            case ConvertibleBond:
                return getConvertibleBondPositionAsOf(inUser,
                                                      inDate,
                                                      (ConvertibleBond)inInstrument);
            case CommonStock:
                return getEquityPositionAsOf(inUser,
                                             inDate,
                                             (Equity)inInstrument);
            case Future:
                return getFuturePositionAsOf(inUser,
                                             inDate,
                                             (Future)inInstrument);
            case Option:
                return getOptionPositionAsOf(inUser,
                                             inDate,
                                             (Option)inInstrument);
            case Unknown:
            default:
                throw new UnsupportedOperationException("Unsupported security type: " + inInstrument.getSecurityType());
        }
    }
    /* (non-Javadoc)
     * @see com.marketcetera.ors.dao.ReportService#getEquityPositionAsOf(com.marketcetera.ors.security.SimpleUser, java.util.Date, org.marketcetera.trade.Equity)
     */
    @Override
    public BigDecimal getEquityPositionAsOf(User inUser,
                                            Date inDate,
                                            Equity inEquity)
    {
        return getPositionAsOf(inUser,
                               inDate,
                               inEquity,
                               new SymbolMatcher<Equity>() {
            @Override
            public BooleanExpression where(QPersistentOrderSummary inTableProxy,
                                           Equity inInstrument)
            {
                return inTableProxy.symbol.eq(inInstrument.getSymbol());
            }
        });
    }
    /* (non-Javadoc)
     * @see com.marketcetera.ors.dao.ReportService#getOpenOrders(com.marketcetera.ors.security.SimpleUser)
     */
    @Override
    public List<ReportBaseImpl> getOpenOrders(User inViewer)
    {
        // TODO this might be an issue for a huge number of open orders
        List<Report> reportValues = orderStatusService.findReportByOrderStatusIn(inViewer,
                                                                                 OrderStatus.openOrderStatuses);
        List<ReportBaseImpl> reports = Lists.newArrayList();
        for(Report report : reportValues) {
            reports.add((ReportBaseImpl)((PersistentReport)report).toReport());
        }
        return reports;
    }
    /* (non-Javadoc)
     * @see org.marketcetera.trade.service.ReportService#getAllPositionsAsOf(org.marketcetera.admin.User, java.util.Date)
     */
    @Override
    public Map<PositionKey<? extends Instrument>,BigDecimal> getAllPositionsAsOf(User inUser,
                                                                                 Date inDate)
    {
        Map<PositionKey<? extends Instrument>,BigDecimal> results = Maps.newHashMap();
        results.putAll(getAllEquityPositionsAsOf(inUser,
                                                 inDate));
        results.putAll(getAllCurrencyPositionsAsOf(inUser,
                                                   inDate));
        results.putAll(getAllConvertibleBondPositionsAsOf(inUser,
                                                          inDate));
        results.putAll(getAllFuturePositionsAsOf(inUser,
                                                 inDate));
        results.putAll(getAllOptionPositionsAsOf(inUser,
                                                 inDate));
        return results;
    }
    /* (non-Javadoc)
     * @see com.marketcetera.ors.dao.ReportService#getAllEquityPositionsAsOf(com.marketcetera.ors.security.SimpleUser, java.util.Date)
     */
    @Override
    public Map<PositionKey<Equity>,BigDecimal> getAllEquityPositionsAsOf(User inUser,
                                                                         Date inDate)
    {
        return getAllPositionsAsOf(inDate,
                                   inUser,
                                   SecurityType.CommonStock,
                                   new PositionTransformer<Equity>() {
            @Override
            public PositionKey<Equity> createPositionKey(String inSymbol,
                                                         String inExpiry,
                                                         BigDecimal inStrikePrice,
                                                         OptionType inOptionType,
                                                         String inAccount,
                                                         Long inTraderId)
            {
                return PositionKeyFactory.createEquityKey(inSymbol,
                                                          inAccount,
                                                          inTraderId == null ? null : String.valueOf(inTraderId));
            }}
        );
    }
    /* (non-Javadoc)
     * @see com.marketcetera.ors.dao.ReportService#getCurrencyPositionAsOf(com.marketcetera.ors.security.SimpleUser, java.util.Date, org.marketcetera.trade.Currency)
     */
    @Override
    public BigDecimal getCurrencyPositionAsOf(User inUser,
                                              Date inDate,
                                              Currency inCurrency)
    {
        return getPositionAsOf(inUser,
                               inDate,
                               inCurrency,
                               new SymbolMatcher<Currency>() {
            @Override
            public BooleanExpression where(QPersistentOrderSummary inTableProxy,
                                           Currency inInstrument)
            {
                return inTableProxy.symbol.eq(inInstrument.getSymbol());
            }
        });
    }
    /* (non-Javadoc)
     * @see com.marketcetera.ors.dao.ReportService#getAllCurrencyPositionsAsOf(com.marketcetera.ors.security.SimpleUser, java.util.Date)
     */
    @Override
    public Map<PositionKey<Currency>,BigDecimal> getAllCurrencyPositionsAsOf(User inUser,
                                                                             Date inDate)
    {
        return getAllPositionsAsOf(inDate,
                                   inUser,
                                   SecurityType.Currency,
                                   new PositionTransformer<Currency>() {
            @Override
            public PositionKey<Currency> createPositionKey(String inSymbol,
                                                           String inExpiry,
                                                           BigDecimal inStrikePrice,
                                                           OptionType inOptionType,
                                                           String inAccount,
                                                           Long inTraderId)
            {
                return PositionKeyFactory.createCurrencyKey(inSymbol,
                                                            inAccount,
                                                            inTraderId == null ? null : String.valueOf(inTraderId));
            }}
        );
    }
    /* (non-Javadoc)
     * @see com.marketcetera.ors.dao.ReportService#getAllFuturePositionsAsOf(com.marketcetera.ors.security.SimpleUser, java.util.Date)
     */
    @Override
    public Map<PositionKey<Future>,BigDecimal> getAllFuturePositionsAsOf(User inUser,
                                                                         Date inDate)
    {
        return getAllPositionsAsOf(inDate,
                                   inUser,
                                   SecurityType.Future,
                                   new PositionTransformer<Future>() {
            @Override
            public PositionKey<Future> createPositionKey(String inSymbol,
                                                         String inExpiry,
                                                         BigDecimal inStrikePrice,
                                                         OptionType inOptionType,
                                                         String inAccount,
                                                         Long inTraderId)
            {
                return PositionKeyFactory.createFutureKey(inSymbol,
                                                          inExpiry,
                                                          inAccount,
                                                          inTraderId == null ? null : String.valueOf(inTraderId));
            }}
        );
    }
    /* (non-Javadoc)
     * @see com.marketcetera.ors.dao.ReportService#getAllConvertibleBondPositionsAsOf(com.marketcetera.ors.security.SimpleUser, java.util.Date)
     */
    @Override
    public Map<PositionKey<ConvertibleBond>,BigDecimal> getAllConvertibleBondPositionsAsOf(User inUser,
                                                                                           Date inDate)
    {
        return getAllPositionsAsOf(inDate,
                                   inUser,
                                   SecurityType.ConvertibleBond,
                                   new PositionTransformer<ConvertibleBond>() {
            @Override
            public PositionKey<ConvertibleBond> createPositionKey(String inSymbol,
                                                                  String inExpiry,
                                                                  BigDecimal inStrikePrice,
                                                                  OptionType inOptionType,
                                                                  String inAccount,
                                                                  Long inTraderId)
            {
                return PositionKeyFactory.createConvertibleBondKey(inSymbol,
                                                                   inAccount,
                                                                   inTraderId == null ? null : String.valueOf(inTraderId));
            }}
        );
    }
    /* (non-Javadoc)
     * @see com.marketcetera.ors.dao.ReportService#getConvertibleBondPositionAsOf(com.marketcetera.ors.security.SimpleUser, java.util.Date, org.marketcetera.trade.ConvertibleBond)
     */
    @Override
    public BigDecimal getConvertibleBondPositionAsOf(User inUser,
                                                     Date inDate,
                                                     ConvertibleBond inConvertibleBond)
    {
        return getPositionAsOf(inUser,
                               inDate,
                               inConvertibleBond,
                               new SymbolMatcher<ConvertibleBond>() {
            @Override
            public BooleanExpression where(QPersistentOrderSummary inTableProxy,
                                           ConvertibleBond inInstrument)
            {
                return inTableProxy.symbol.eq(inInstrument.getSymbol());
            }
        });
    }
    /* (non-Javadoc)
     * @see com.marketcetera.ors.dao.ReportService#getFuturePositionAsOf(com.marketcetera.ors.security.SimpleUser, java.util.Date, org.marketcetera.trade.Future)
     */
    @Override
    public BigDecimal getFuturePositionAsOf(User inUser,
                                            Date inDate,
                                            Future inFuture)
    {
        return getPositionAsOf(inUser,
                               inDate,
                               inFuture,
                               new SymbolMatcher<Future>() {
            @Override
            public BooleanExpression where(QPersistentOrderSummary inTableProxy,
                                           Future inInstrument)
            {
                return inTableProxy.symbol.eq(inInstrument.getSymbol()).and(inTableProxy.expiry.eq(inInstrument.getExpiryAsString()));
            }
        });
    }
    /* (non-Javadoc)
     * @see com.marketcetera.ors.dao.ReportService#getOptionPositionAsOf(com.marketcetera.ors.security.SimpleUser, java.util.Date, org.marketcetera.trade.Option)
     */
    @Override
    public BigDecimal getOptionPositionAsOf(User inUser,
                                            Date inDate,
                                            Option inOption)
    {
        return getPositionAsOf(inUser,
                               inDate,
                               inOption,
                               new SymbolMatcher<Option>() {
            @Override
            public BooleanExpression where(QPersistentOrderSummary inTableProxy,
                                           Option inInstrument)
            {
                return inTableProxy.symbol.eq(inInstrument.getSymbol())
                        .and(inTableProxy.expiry.eq(inInstrument.getExpiry()))
                        .and(inTableProxy.strikePrice.eq(inInstrument.getStrikePrice()))
                        .and(inTableProxy.optionType.eq(inInstrument.getType()));
            }
        });
    }
    /* (non-Javadoc)
     * @see com.marketcetera.ors.dao.ReportService#getAllOptionPositionsAsOf(com.marketcetera.ors.security.SimpleUser, java.util.Date)
     */
    @Override
    public Map<PositionKey<Option>,BigDecimal> getAllOptionPositionsAsOf(User inUser,
                                                                         Date inDate)
    {
        return getAllPositionsAsOf(inDate,
                                   inUser,
                                   SecurityType.Option,
                                   new PositionTransformer<Option>() {
            @Override
            public PositionKey<Option> createPositionKey(String inSymbol,
                                                         String inExpiry,
                                                         BigDecimal inStrikePrice,
                                                         OptionType inOptionType,
                                                         String inAccount,
                                                         Long inTraderId)
            {
                return PositionKeyFactory.createOptionKey(inSymbol,
                                                          inExpiry,
                                                          inStrikePrice,
                                                          inOptionType,
                                                          inAccount,
                                                          inTraderId == null ? null : String.valueOf(inTraderId));
            }});
    }
    /* (non-Javadoc)
     * @see com.marketcetera.ors.dao.ReportService#getOptionPositionsAsOf(com.marketcetera.ors.security.SimpleUser, java.util.Date, java.lang.String[])
     */
    @Override
    public Map<PositionKey<Option>,BigDecimal> getOptionPositionsAsOf(User inUser,
                                                                      Date inDate,
                                                                      String[] inSymbols)
    {
        return getAllPositionsAsOf(inDate,
                                   inUser,
                                   SecurityType.Option,
                                   new PositionTransformer<Option>() {
            @Override
            public PositionKey<Option> createPositionKey(String inSymbol,
                                                         String inExpiry,
                                                         BigDecimal inStrikePrice,
                                                         OptionType inOptionType,
                                                         String inAccount,
                                                         Long inTraderId)
            {
                return PositionKeyFactory.createOptionKey(inSymbol,
                                                          inExpiry,
                                                          inStrikePrice,
                                                          inOptionType,
                                                          inAccount,
                                                          inTraderId == null ? null : String.valueOf(inTraderId));
            }},inSymbols);
    }
    /* (non-Javadoc)
     * @see com.marketcetera.ors.dao.ReportService#save(org.marketcetera.trade.ReportBase)
     */
    @Override
    @Transactional(readOnly=false,propagation=Propagation.REQUIRED)
    public PersistentReport save(ReportBase inReport)
    {
        PersistentReport newReport = new PersistentReport(inReport,
                                                          inReport.getActorID() == null ? null : (PersistentUser)userService.findOne(inReport.getActorID().getValue()),
                                                          inReport.getViewerID() == null ? null : (PersistentUser)userService.findOne(inReport.getViewerID().getValue()));
        // check to see if the report already exists. if it does, just return it.
        BooleanBuilder where = new BooleanBuilder();
        where = where.and(QPersistentReport.persistentReport.msgSeqNum.eq(newReport.getMsgSeqNum()));
        if(inReport.getOrderID() != null) {
            where = where.and(QPersistentReport.persistentReport.orderID.eq(inReport.getOrderID()));
        }
        if(newReport.getSessionId() != null) {
            where = where.and(QPersistentReport.persistentReport.sessionIdValue.eq(newReport.getSessionId().toString()));
        }
        Date sessionStart = getSessionStart(newReport.getSessionId());
        if(sessionStart == null) {
            sessionStart = new Date(0);
        }
        where = where.and(QPersistentReport.persistentReport.sendingTime.goe(getSessionStart(newReport.getSessionId())));
        Optional<PersistentReport> reportOption = persistentReportDao.findOne(where);
        PersistentReport report;
        if(reportOption.isPresent()) {
            SLF4JLoggerProxy.debug(this,
                                   "Using existing report {} for {}",
                                   reportOption,
                                   inReport);
            // reset the report id to the original report id
            report = reportOption.get();
            ReportBaseImpl.assignReportID((ReportBaseImpl)inReport,
                                          report.getReportID());
            return report;
        }
        report = persistentReportDao.save(newReport);
        OrderID rootID = rootOrderIdFactory.getRootOrderId((TradeMessage)inReport);
        if(report.getReportType() == ReportType.ExecutionReport) {
            PersistentExecutionReport reportSummary = new PersistentExecutionReport((ExecutionReport)inReport,
                                                                                    report);
            reportSummary.setRootOrderID(rootID);
            reportSummary = executionReportDao.save(reportSummary);
        }
        // update order summary record
        try {
            generateOrderSummary(inReport,
                                 rootID,
                                 report);
        } catch (Exception e) {
            if(SLF4JLoggerProxy.isDebugEnabled(this)) {
                SLF4JLoggerProxy.warn(this,
                                      e,
                                      "Unable to create or update the order status record for {}: {}",
                                      inReport,
                                      ExceptionUtils.getRootCauseMessage(e));
            } else {
                SLF4JLoggerProxy.warn(this,
                                      "Unable to create or update the order status record for {}: {}",
                                      inReport,
                                      ExceptionUtils.getRootCauseMessage(e));
            }
        }
        return report;
    }
    /* (non-Javadoc)
     * @see org.marketcetera.trade.service.ReportService#delete(org.marketcetera.trade.ReportID)
     */
    @Override
    @Transactional(readOnly=false,propagation=Propagation.REQUIRED)
    public void delete(ReportID inReportId)
    {
        PersistentReport reportToDelete = persistentReportDao.findByReportID(inReportId);
        if(reportToDelete == null) {
            throw new IllegalArgumentException("No report for report ID " + inReportId);
        }
        OrderID rootId = null;
        PersistentExecutionReport mostRecentExecReport = null;
        PersistentExecutionReport execReport = executionReportDao.findByReportId(reportToDelete.getId());
        // this might be null and that's ok, not an error - the report might not be an exec report
        if(execReport != null) {
            rootId = execReport.getRootOrderID();
            executionReportDao.delete(execReport);
            Page<PersistentExecutionReport> result = executionReportDao.findMostRecentReportFor(rootId,
                                                                                                PageRequest.of(0,1));
            if(result.hasContent()) {
                mostRecentExecReport = result.getContent().get(0);
            }
        }
        // next, delete the order status
        OrderSummary orderStatus = orderStatusService.findByReportId(reportToDelete.getId());
        if(orderStatus != null) {
            orderStatusService.delete(orderStatus);
        }
        // delete the report
        persistentReportDao.delete(reportToDelete);
        // recreate the order status if there is a most recent report
        if(mostRecentExecReport != null) {
            PersistentReport mostRecentReport = mostRecentExecReport.getReport();
            ReportBase reportBase = mostRecentReport.toReport();
            generateOrderSummary(reportBase,
                                 rootId,
                                 mostRecentReport);
        }
    }
    /* (non-Javadoc)
     * @see com.marketcetera.ors.dao.ReportService#getRootOrderIdFor(org.marketcetera.trade.OrderID)
     */
    @Override
    public OrderID getRootOrderIdFor(OrderID inOrderId)
    {
        return executionReportDao.findRootIDForOrderID(inOrderId);
    }
    /* (non-Javadoc)
     * @see com.marketcetera.ors.brokers.FixSessionListener#sessionDisabled(com.marketcetera.ors.brokers.FixSession)
     */
    @Override
    @Transactional(readOnly=true,propagation=Propagation.SUPPORTS)
    public void sessionDisabled(FixSession inSession)
    {
        SessionID sessionId = new SessionID(inSession.getSessionId());
        cachedSessionStart.invalidate(sessionId);
    }
    /* (non-Javadoc)
     * @see com.marketcetera.ors.brokers.FixSessionListener#sessionEnabled(com.marketcetera.ors.brokers.FixSession)
     */
    @Override
    @Transactional(readOnly=true,propagation=Propagation.SUPPORTS)
    public void sessionEnabled(FixSession inSession)
    {
        SessionID sessionId = new SessionID(inSession.getSessionId());
        cachedSessionStart.invalidate(sessionId);
    }
    /* (non-Javadoc)
     * @see com.marketcetera.ors.brokers.FixSessionListener#sessionStopped(com.marketcetera.ors.brokers.FixSession)
     */
    @Override
    @Transactional(readOnly=true,propagation=Propagation.SUPPORTS)
    public void sessionStopped(FixSession inSession)
    {
        SessionID sessionId = new SessionID(inSession.getSessionId());
        cachedSessionStart.invalidate(sessionId);
    }
    /* (non-Javadoc)
     * @see com.marketcetera.ors.brokers.FixSessionListener#sessionStarted(com.marketcetera.ors.brokers.FixSession)
     */
    @Override
    @Transactional(readOnly=true,propagation=Propagation.SUPPORTS)
    public void sessionStarted(FixSession inSession)
    {
        SessionID sessionId = new SessionID(inSession.getSessionId());
        cachedSessionStart.invalidate(sessionId);
    }
    /* (non-Javadoc)
     * @see org.marketcetera.trade.service.ReportService#assignReportId(org.marketcetera.trade.HasMutableReportID)
     */
    @Override
    @Transactional(readOnly=false,propagation=Propagation.REQUIRED)
    public void assignReportId(HasMutableReportID inReport)
    {
        inReport.setReportID(new ReportID(reportIDFactory.getNext()));
    }
    /**
     * Get the missingSeqNumBatchSize value.
     *
     * @return a <code>int</code> value
     */
    public int getMissingSeqNumBatchSize()
    {
        return missingSeqNumBatchSize;
    }
    /**
     * Sets the missingSeqNumBatchSize value.
     *
     * @param inMissingSeqNumBatchSize a <code>int</code> value
     */
    public void setMissingSeqNumBatchSize(int inMissingSeqNumBatchSize)
    {
        missingSeqNumBatchSize = inMissingSeqNumBatchSize;
    }
    /**
     * Get the userService value.
     *
     * @return a <code>UserService</code> value
     */
    public UserService getUserService()
    {
        return userService;
    }
    /**
     * Sets the userService value.
     *
     * @param inUserService a <code>UserService</code> value
     */
    public void setUserService(UserService inUserService)
    {
        userService = inUserService;
    }
    /**
     * Get the executionReportDao value.
     *
     * @return a <code>ExecutionReportDao</code> value
     */
    public ExecutionReportDao getExecutionReportDao()
    {
        return executionReportDao;
    }
    /**
     * Sets the executionReportDao value.
     *
     * @param inExecutionReportDao a <code>ExecutionReportDao</code> value
     */
    public void setExecutionReportDao(ExecutionReportDao inExecutionReportDao)
    {
        executionReportDao = inExecutionReportDao;
    }
    /**
     * Get the persistentReportDao value.
     *
     * @return a <code>PersistentReportDao</code> value
     */
    public PersistentReportDao getPersistentReportDao()
    {
        return persistentReportDao;
    }
    /**
     * Sets the persistentReportDao value.
     *
     * @param inPersistentReportDao a <code>PersistentReportDao</code> value
     */
    public void setPersistentReportDao(PersistentReportDao inPersistentReportDao)
    {
        persistentReportDao = inPersistentReportDao;
    }
    /**
     * Get the entityManager value.
     *
     * @return a <code>EntityManager</code> value
     */
    public EntityManager getEntityManager()
    {
        return entityManager;
    }
    /**
     * Sets the entityManager value.
     *
     * @param inEntityManager a <code>EntityManager</code> value
     */
    public void setEntityManager(EntityManager inEntityManager)
    {
        entityManager = inEntityManager;
    }
    /**
     * Get the cacheSize value.
     *
     * @return an <code>int</code> value
     */
    public int getCacheSize()
    {
        return cacheSize;
    }
    /**
     * Sets the cacheSize value.
     *
     * @param an <code>int</code> value
     */
    public void setCacheSize(int inCacheSize)
    {
        cacheSize = inCacheSize;
    }
    /**
     * Build the sort statement for a query using the given attributes.
     *
     * @param inPageRequest an <code>org.marketcetera.persist.PageRequest</code> value
     * @param inTableAliases a <code>Map&lt;String,String&gt;</code> value
     * @param inDefaultSort a <code>Sort</code> value
     * @return a <code>Sort</code> value
     */
    private Sort buildSort(org.marketcetera.persist.PageRequest inPageRequest,
                           Map<String,String> inTableAliases,
                           Sort inDefaultSort)
    {
        if(inPageRequest.getSortOrder() == null || inPageRequest.getSortOrder().isEmpty()) {
            return inDefaultSort;
        } else {
            List<Sort.Order> specifiedSorts = Lists.newArrayList();
            for(org.marketcetera.persist.Sort requestedSort : inPageRequest.getSortOrder()) {
                String property = requestedSort.getProperty();
                String aliasDefinition = inTableAliases.get(property.toLowerCase());
                if(aliasDefinition == null) {
                    specifiedSorts.add(new Sort.Order(requestedSort.getDirection().getSpringSortDirection(),
                                                      property));
                } else {
                    for(String alias : aliasDefinition.split(",")) {
                        specifiedSorts.add(new Sort.Order(requestedSort.getDirection().getSpringSortDirection(),
                                                          alias));
                    }
                }
            }
            return Sort.by(specifiedSorts);
        }
    }
    /**
     * Generate and persist order summary for the given artifacts.
     *
     * @param inReportBase a <code>ReportBase</code> value
     * @param inRootId an <code>OrderID</code> value
     * @param inReport a <code>PersistentReport</code> value
     */
    private void generateOrderSummary(ReportBase inReportBase,
                                      OrderID inRootId,
                                      PersistentReport inReport)
    {
        OrderSummary orderSummary;
        if(inReportBase instanceof OrderCancelReject) {
            // need to search for some particulars to help us fill out this record
            orderSummary = orderStatusService.findMostRecentExecutionByRootOrderId(inRootId);
        } else {
            orderSummary = orderStatusService.findByRootOrderIdAndOrderId(inRootId,
                                                                          inReportBase.getOrderID());
            if(orderSummary == null && inReportBase.getOriginalOrderID() != null) {
                orderSummary = orderStatusService.findByRootOrderIdAndOrderId(inRootId,
                                                                              inReportBase.getOriginalOrderID());
            }
        }
        if(orderSummary == null) {
            orderSummary = new PersistentOrderSummary(inReport,
                                                      inReportBase,
                                                      inRootId);
            orderSummary = orderStatusService.save(orderSummary);
        } else {
            orderSummary = orderStatusService.update(orderSummary,
                                                     inReport,
                                                     inReportBase);
        }
        eventBusService.post(new SimpleOrderSummaryEvent(orderSummary));
    }
    /**
     * Get the planned session start for the given session id.
     *
     * @param inSessionId a <code>SessionID</code> value
     * @return a <code>Date</code> value
     */
    private Date getSessionStart(final SessionID inSessionId)
    {
        Date sessionStart = null;
        try {
            sessionStart = cachedSessionStart.getIfPresent(inSessionId);
            if(sessionStart == null) {
                sessionStart = brokerService.getSessionStart(inSessionId);
                if(sessionStart != null) {
                    cachedSessionStart.put(inSessionId,
                                           sessionStart);
                    Date nextSessionStart = brokerService.getNextSessionStart(inSessionId);
                    if(nextSessionStart != null) {
                        timerService.schedule(new TimerTask() {
                            @Override
                            public void run()
                            {
                                try {
                                    cachedSessionStart.invalidate(inSessionId);
                                } catch (Exception e) {
                                    String message = "Could not invalidate session start for " + inSessionId + ": " + ExceptionUtils.getRootCauseMessage(e);
                                    if(SLF4JLoggerProxy.isDebugEnabled(this)) {
                                        SLF4JLoggerProxy.warn(this,
                                                              e,
                                                              message);
                                    } else {
                                        SLF4JLoggerProxy.warn(this,
                                                              message);
                                    }
                                }
                            }
                        },nextSessionStart);
                    }
                }
            }
        } catch (Exception e) {
            String message = "Could not determine session start for " + inSessionId + ": " + ExceptionUtils.getRootCauseMessage(e);
            if(SLF4JLoggerProxy.isDebugEnabled(this)) {
                SLF4JLoggerProxy.warn(this,
                                      e,
                                      message);
            } else {
                SLF4JLoggerProxy.warn(this,
                                      message);
            }
        }
        if(sessionStart == null) {
            sessionStart = new DateTime().withTimeAtStartOfDay().toDate();
            SLF4JLoggerProxy.debug(this,
                                   "No session start for {}, using start of day: {}",
                                   inSessionId,
                                   sessionStart);
        }
        return sessionStart;
    }
    /**
     * Executes an all positions query by instrument type.
     *
     * @param inAsOfDate a <code>Date</code> value
     * @param inViewer a <code>SimpleUser</code> value
     * @param inSecurityType a <code>SecurityType</code> value
     * @param inTupleTransformer a <code>PositionTransformer&lt;I&gt;</code> value
     * @param inSymbols a <code>String[]</code> value
     * @return a <code>Map&lt;PositionKey&lt;I&gt;,BigDecimal&gt;</code> value
     */
    private <I extends Instrument> Map<PositionKey<I>,BigDecimal> getAllPositionsAsOf(Date inAsOfDate,
                                                                                      User inViewer,
                                                                                      SecurityType inSecurityType,
                                                                                      PositionTransformer<I> inTupleTransformer,
                                                                                      String...inSymbols)
    {
        QPersistentOrderSummary o = QPersistentOrderSummary.persistentOrderSummary;
        JPAQueryFactory jpaQueryFactory = new JPAQueryFactory(entityManager);
        BooleanBuilder primaryWhere = new BooleanBuilder();
        primaryWhere = primaryWhere.and(o.transactTime.loe(inAsOfDate));
        primaryWhere = primaryWhere.and(o.securityType.eq(inSecurityType));
        if(inSymbols != null && inSymbols.length != 0) {
            primaryWhere = primaryWhere.and(o.symbol.in(inSymbols));
        }
        BooleanBuilder permissionWhere = new BooleanBuilder();
        Set<User> basicUsers = authzService.getSubjectUsersFor(inViewer,
                                                               TradePermissions.ViewPositionAction.name());
        Set<PersistentUser> subjectUsers = Sets.newHashSet();
        for(User basicUser : basicUsers) {
            subjectUsers.add((PersistentUser)basicUser);
        }
        // show the position if the given user is this user or has supervisor permission over the position (the position permission has already been checked for the "same user" case)
        if(subjectUsers.isEmpty()) {
            permissionWhere = permissionWhere.and(permissionWhere.and(o.viewer.eq((PersistentUser)inViewer)));
        } else {
            permissionWhere = permissionWhere.and(permissionWhere.and(o.viewer.in(subjectUsers)).or(o.viewer.eq((PersistentUser)inViewer)));
        }
        primaryWhere = primaryWhere.and(permissionWhere);
        List<Tuple> results = jpaQueryFactory.select(o.cumulativeQuantity,o.symbol,o.expiry,o.strikePrice,o.optionType,o.account,o.actor.id).from(o).where(primaryWhere).fetch();
        Map<PositionKey<I>,BigDecimal> finalResults = new LinkedHashMap<PositionKey<I>,BigDecimal>();
        if(results != null) {
            for(Tuple result : results) {
                PositionKey<I> key = inTupleTransformer.createPositionKey(result.get(o.symbol),
                                                                          result.get(o.expiry),
                                                                          result.get(o.strikePrice),
                                                                          result.get(o.optionType),
                                                                          result.get(o.account),
                                                                          result.get(o.actor.id));
                BigDecimal quantity = finalResults.get(key);
                if(quantity == null) {
                    quantity = BigDecimal.ZERO;
                }
                quantity = quantity.add(result.get(o.cumulativeQuantity));
                finalResults.put(key,
                                 quantity);
            }
        }
        return finalResults;
    }
    /**
     * Executes a position query for the given instrument.
     *
     * @param inUser a <code>SimpleUser</code> value
     * @param inDate a <code>Date</code> value
     * @param inInstrument an <code>I</code> value
     * @param inSymbolMatcher a <code>SymbolMatcher&lt;I&gt;</code> value
     * @return a <code>BigDecimal</code> value
     */
    private <I extends Instrument> BigDecimal getPositionAsOf(User inUser,
                                                              Date inDate,
                                                              I inInstrument,
                                                              SymbolMatcher<I> inSymbolMatcher)
    {
        QPersistentOrderSummary o = QPersistentOrderSummary.persistentOrderSummary;
        JPAQueryFactory jpaQueryFactory = new JPAQueryFactory(entityManager);
        BooleanBuilder primaryWhere = new BooleanBuilder().and(inSymbolMatcher.where(o,
                                                                                     inInstrument));
        primaryWhere = primaryWhere.and(o.securityType.eq(inInstrument.getSecurityType()));
        primaryWhere = primaryWhere.and(o.transactTime.loe(inDate));
        BooleanBuilder permissionWhere = new BooleanBuilder();
        Set<User> basicUsers = authzService.getSubjectUsersFor(inUser,
                                                               TradePermissions.ViewPositionAction.name());
        Set<PersistentUser> subjectUsers = Sets.newHashSet();
        for(User basicUser : basicUsers) {
            subjectUsers.add((PersistentUser)basicUser);
        }
        if(subjectUsers.isEmpty()) {
            permissionWhere = permissionWhere.and(o.viewer.eq((PersistentUser)inUser));
        } else {
            permissionWhere = permissionWhere.and(o.viewer.in(subjectUsers)).or(o.viewer.eq((PersistentUser)inUser));
        }
        primaryWhere = primaryWhere.and(permissionWhere);
        BigDecimal result = jpaQueryFactory.select(o.cumulativeQuantity.sum()).from(o).where(primaryWhere).fetchOne();
        if(result == null) {
            result = BigDecimal.ZERO;
        }
        return result;
    }
    /**
     * Translates a position tuple to a position key.
     *
     * @author <a href="mailto:colin@marketcetera.com">Colin DuPlantis</a>
     * @version $Id$
     * @since 2.4.2
     */
    @ClassVersion("$Id$")
    private interface PositionTransformer<I extends Instrument>
    {
        /**
         * Creates a <code>PositionKey</code> from the given inputs.
         *
         * @param inSymbol a <code>String</code> value
         * @param inExpiry a <code>String</code> value
         * @param inStrikePrice a <code>BigDecimal</code> value
         * @param inOptionType an <code>OptionType</code> value
         * @param inAccount a <code>String</code> value
         * @param inTraderId a <code>Long</code> value
         * @return a <code>PositionKey&lt;I&gt; value
         */
        PositionKey<I> createPositionKey(String inSymbol,
                                         String inExpiry,
                                         BigDecimal inStrikePrice,
                                         OptionType inOptionType,
                                         String inAccount,
                                         Long inTraderId);
    }
    /**
     * Creates a predicate used to match rows to a given instrument.
     *
     * @author <a href="mailto:colin@marketcetera.com">Colin DuPlantis</a>
     * @version $Id$
     * @since 2.4.2
     */
    @ClassVersion("$Id$")
    private interface SymbolMatcher<I extends Instrument>
    {
        /**
         * Builds a where predicate that matches records to the given instrument.
         *
         * @param inTableProxy a <code>QPersistentOrderSummary</code> value
         * @param inInstrument an <code>I</code> value
         * @return a <code>BooleanExpression</code> value
         */
        BooleanExpression where(QPersistentOrderSummary inTableProxy,
                                I inInstrument);
    }
    /**
     * caches session start for a session
     */
    private Cache<SessionID,Date> cachedSessionStart;
    /**
     * number of order principals to cache
     */
    private int cacheSize = 1000;
    /**
     * page size for finding missing seq num in the reports table
     */
    private int missingSeqNumBatchSize = 1000;
    /**
     * provides datastore access to users
     */
    @Autowired
    private UserService userService;
    /**
     * provides datastore access to order status
     */
    @Autowired
    private OrderSummaryService orderStatusService;
    /**
     * provides datastore access to execution reports
     */
    @Autowired
    private ExecutionReportDao executionReportDao;
    /**
     * provides datastore access to persistent reports
     */
    @Autowired
    private PersistentReportDao persistentReportDao;
    /**
     * provides root order ID values
     */
    @Autowired
    private RootOrderIdFactory rootOrderIdFactory;
    /**
     * provides data store access to incoming FIX messages
     */
    @Autowired
    private IncomingMessageDao incomingMessageDao;
    /**
     * creates {@link AverageFillPrice} objects
     */
    @Autowired
    private AverageFillPriceFactory averageFillPriceFactory;
    /**
     * resolves symbols to {@link Instrument} objects
     */
    @Autowired
    private SymbolResolverService symbolResolverService;
    /**
     * entity manager value used to construct queries
     */
    @PersistenceContext
    private EntityManager entityManager;
    /**
     * provides access to broker services
     */
    @Autowired
    private BrokerService brokerService;
    /**
     * provides access to event bus services
     */
    @Autowired
    private EventBusService eventBusService;
    /**
     * provides access to authorization services
     */
    @Autowired
    private AuthorizationService authzService;
    /**
     * provides an interface to long-based ids
     */
    private LongIDFactory reportIDFactory;
    /**
     * provides access to unique IDs
     */
    @Autowired
    private IDFactory idFactory;
    /**
     * provides scheduled services
     */
    private Timer timerService;
    /**
     * map of styles specified in configuration
     */
    @Value("#{${metc.persistent.report.aliases}}")
    private Map<String,String> persistentReportAliases = Maps.newHashMap();
    /**
     * map of styles specified in configuration
     */
    @Value("#{${metc.persistent.execution.report.aliases}}")
    private Map<String,String> persistentExecutionReportAliases = Maps.newHashMap();
}
