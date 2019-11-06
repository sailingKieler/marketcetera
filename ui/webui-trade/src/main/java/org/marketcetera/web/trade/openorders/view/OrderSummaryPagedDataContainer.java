package org.marketcetera.web.trade.openorders.view;

import java.util.Collection;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.marketcetera.persist.CollectionPageResponse;
import org.marketcetera.persist.PageRequest;
import org.marketcetera.trade.OrderSummary;
import org.marketcetera.web.service.trade.TradeClientService;
import org.marketcetera.web.view.PagedDataContainer;
import org.marketcetera.web.view.PagedViewProvider;

/* $License$ */

/**
 * Provides a <code>PagedDataContainer</code> implementation for <code>OrderSummary</code> values.
 *
 * @author <a href="mailto:colin@marketcetera.com">Colin DuPlantis</a>
 * @version $Id$
 * @since $Release$
 */
public class OrderSummaryPagedDataContainer
        extends PagedDataContainer<OrderSummary>
{
    /**
     * Create a new OrderSummaryPagedDataContainer instance.
     *
     * @param inType a <code>Class&lt; ? extends OrderSummary&gt;</code> value
     * @param inCollection a <code>Collection&lt;? extends OrderSummary&gt;</code> value
     * @param inPagedViewProvider a <code>PagedViewProvider</code> value
     * @throws IllegalArgumentException if the container cannot be constructed
     */
    public OrderSummaryPagedDataContainer(Collection<? extends OrderSummary> inCollection,
                                          PagedViewProvider inPagedViewProvider)
            throws IllegalArgumentException
    {
        super(OrderSummary.class,
              inCollection,
              inPagedViewProvider);
    }
    /**
     * Create a new OrderSummaryPagedDataContainer instance.
     *
     * @param inType a <code>Class&lt; super OrderSummary&gt;</code> value
     * @param inPagedViewProvider a <code>PagedViewProvider</code> value
     * @throws IllegalArgumentException if the container cannot be constructed
     */
    public OrderSummaryPagedDataContainer(PagedViewProvider inPagedViewProvider)
            throws IllegalArgumentException
    {
        super(OrderSummary.class,
              inPagedViewProvider);
    }
    /* (non-Javadoc)
     * @see com.marketcetera.web.view.PagedDataContainer#getDataContainerContents(org.marketcetera.core.PageRequest)
     */
    @Override
    protected CollectionPageResponse<OrderSummary> getDataContainerContents(PageRequest inPageRequest)
    {
        return TradeClientService.getInstance().getOpenOrders(inPageRequest);
    }
    /* (non-Javadoc)
     * @see com.marketcetera.web.view.PagedDataContainer#isDeepEquals(java.lang.Object, java.lang.Object)
     */
    @Override
    protected boolean isDeepEquals(OrderSummary inO1,
                                   OrderSummary inO2)
    {
        // include values here that are likely to change over the course on an order
        return new EqualsBuilder().append(inO1.getAccount(),inO2.getAccount())
                .append(inO1.getAveragePrice(),inO2.getAveragePrice())
                .append(inO1.getCumulativeQuantity(),inO2.getCumulativeQuantity())
                .append(inO1.getLastPrice(),inO2.getLastPrice())
                .append(inO1.getLastQuantity(),inO2.getLastQuantity())
                .append(inO1.getLeavesQuantity(),inO2.getLeavesQuantity())
                .append(inO1.getOrderId(),inO2.getOrderId())
                .append(inO1.getOrderPrice(),inO2.getOrderPrice())
                .append(inO1.getOrderQuantity(),inO2.getOrderQuantity())
                .append(inO1.getOrderStatus(),inO2.getOrderStatus())
                .append(inO1.getSendingTime(),inO2.getSendingTime())
                .append(inO1.getSide(),inO2.getSide())
                .append(inO1.getTransactTime(),inO2.getTransactTime()).isEquals();
    }
    /* (non-Javadoc)
     * @see com.marketcetera.web.view.PagedDataContainer#getDescription()
     */
    @Override
    protected String getDescription()
    {
        return "OpenOrders";
    }
    private static final long serialVersionUID = -1643583263489594148L;
}