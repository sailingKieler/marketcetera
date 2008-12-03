package org.marketcetera.strategy;

import static org.marketcetera.strategy.Status.ERROR;
import static org.marketcetera.strategy.Status.NOT_RUNNING;
import static org.marketcetera.strategy.Status.RUNNING;
import static org.marketcetera.strategy.Messages.RUNTIME_ERROR;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.marketcetera.core.ClassVersion;
import org.marketcetera.event.AskEvent;
import org.marketcetera.event.BidEvent;
import org.marketcetera.event.TradeEvent;
import org.marketcetera.trade.ExecutionReport;
import org.marketcetera.trade.OrderCancelReject;

/* $License$ */

/**
 * Representation of a particular strategy.
 * 
 * <p>This class is responsible for tracking the lifecycle and managing the execution
 * of a strategy.  The existence of this object in scope represents the existence
 * of an actual strategy.  If this object is <em>running</em> as indicated by
 * {@link #isRunning()}, then the embedded strategy is running.
 * 
 * <p>The embedded strategy will not begin executing until {@link #start()} is invoked.
 * The strategy will continue to execute until stopped or an error occurs.
 * 
 * <p>To make the embedded strategy stop, invoke {@link #stop()} (preferable) or allow
 * this object to go out-of-scope.  If the object is allowed to go out-of-scope without
 * invoking {@link #stop()}, the embedded strategy will not be warned it is stopping.
 * Additionally, the strategy will keep executing until the next garbage-collection,
 * which is not deterministic.  It is good practice to call {@link #stop()} on each
 * strategy at the appropriate time.
 *
 * @author <a href="mailto:colin@marketcetera.com">Colin DuPlantis</a>
 * @version $Id$
 * @since $Release$
 */
@ClassVersion("$Id$")
class StrategyImpl
        implements Strategy
{
    /* (non-Javadoc)
     * @see org.marketcetera.strategy.Strategy#start()
     */
    @Override
    public final void start()
        throws StrategyException
    {
        try {
            setExecutor(getLanguage().getExecutor(this));
            setRunningStrategy(getExecutor().start());
        } catch (Exception e) {
            setStatus(ERROR);
            throw new StrategyException(e);
        }
        setStatus(RUNNING);
    }
    /* (non-Javadoc)
     * @see org.marketcetera.strategy.Strategy#stop()
     */
    @Override
    public final void stop()
        throws Exception
    {
        try {
            getExecutor().stop();
        } finally {
            setStatus(NOT_RUNNING);
        }
    }
    /* (non-Javadoc)
     * @see org.marketcetera.strategy.Strategy#dataReceived(java.lang.Object)
     */
    @Override
    public final void dataReceived(Object inData)
    {
        String method = "onOther"; //$NON-NLS-1$
        try {
            RunningStrategy runningStrategy = getRunningStrategy();
            if(inData instanceof AskEvent) {
                method = "onAsk"; //$NON-NLS-1$
                runningStrategy.onAsk((AskEvent)inData);
                return;
            }
            if(inData instanceof BidEvent) {
                method = "onBid"; //$NON-NLS-1$
                runningStrategy.onBid((BidEvent)inData);
                return;
            }
            if(inData instanceof OrderCancelReject) {
                method = "onCancel"; //$NON-NLS-1$
                runningStrategy.onCancel((OrderCancelReject)inData);
                return;
            }
            if(inData instanceof ExecutionReport) {
                method = "onExecutionReport"; //$NON-NLS-1$
                if(runningStrategy instanceof AbstractRunningStrategy) {
                    ((AbstractRunningStrategy)runningStrategy).onExecutionReportRedirected((ExecutionReport)inData);
                } else {
                    runningStrategy.onExecutionReport((ExecutionReport)inData);
                }
                return;
            }
            if(inData instanceof TradeEvent) {
                method = "onTrade"; //$NON-NLS-1$
                runningStrategy.onTrade((TradeEvent)inData);
                return;
            }
            // catch-all for every other type of data
            runningStrategy.onOther(inData);
        } catch (Exception e) {
            Executor executor = getExecutor();
            String methodName = method;
            String exceptionTranslation = e.toString();
            if(executor != null) {
                methodName = getExecutor().translateMethodName(method);
                exceptionTranslation = getExecutor().interpretRuntimeException(e);
            }
            RUNTIME_ERROR.warn(Strategy.STRATEGY_MESSAGES,
                               this,
                               methodName,
                               exceptionTranslation);
        }
    }
    /* (non-Javadoc)
     * @see org.marketcetera.strategy.Strategy#getCode()
     */
    @Override
    public final String getScript()
    {
        return code;
    }
    /* (non-Javadoc)
     * @see org.marketcetera.strategy.Strategy#getLanguage()
     */
    @Override
    public final Language getLanguage()
    {
        return language;
    }
    /* (non-Javadoc)
     * @see org.marketcetera.strategy.Strategy#getName()
     */
    @Override
    public final String getName()
    {
        return name;
    }
    /* (non-Javadoc)
     * @see org.marketcetera.strategy.Strategy#getParameters()
     */
    @Override
    public final Properties getParameters()
    {
        return parameters;
    }
    /* (non-Javadoc)
     * @see org.marketcetera.strategy.Strategy#getStatus()
     */
    @Override
    public final Status getStatus()
    {
        return status;
    }
    /**
     * Get the outboundServicesProvider value.
     *
     * @return a <code>OutboundServices</code> value
     */
    @Override
    public final OutboundServicesProvider getOutboundServicesProvider()
    {
        return outboundServicesProvider;
    }
    /**
     * Get the inboundServicesProvider value.
     *
     * @return a <code>StrategyImpl</code> value
     */
    public final InboundServicesProvider getInboundServicesProvider()
    {
        return inboundServicesProvider;
    }
    /* (non-Javadoc)
     * @see org.marketcetera.strategy.Strategy#getClasspath()
     */
    @Override
    public final String[] getClasspath()
    {
        return classpath;
    }
    /* (non-Javadoc)
     * @see org.marketcetera.strategy.Strategy#getDefaultNamespace()
     */
    @Override
    public final String getDefaultNamespace()
    {
        return defaultNamespace;
    }
    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public final String toString()
    {
        return String.format("%s Strategy %s(%s)", //$NON-NLS-1$
                             getLanguage().toString(),
                             getName(),
                             getUniqueIdentifier());
    }
    /**
     * Create a new StrategyImpl instance.
     * 
     * @param inName a <code>String</code> value
     * @param inUniqueIdentifier a <code>String</code> value
     * @param inType a <code>Language</code> value
     * @param inSource a <code>File</code> value
     * @param inParameters a <code>Properties</code> value
     * @param inClasspath a <code>String[]</code> value
     * @param inNamespace a <code>String</code> value 
     * @param inOutboundServicesProvider an <code>OutboundServices</code> value
     * @param inInboundServicesProvider an <code>InboundServices</code> value
     * @throws IOException if the given <code>File</code> could not be resolved
     */
    StrategyImpl(String inName,
                 String inUniqueIdentifier,
                 Language inType,
                 File inSource,
                 Properties inParameters,
                 String[] inClasspath,
                 String inNamespace,
                 OutboundServicesProvider inOutboundServicesProvider,
                 InboundServicesProvider inInboundServicesProvider)
        throws IOException
    {
        status = NOT_RUNNING;
        name = inName;
        uniqueIdentifier = inUniqueIdentifier;
        language = inType;
        source = inSource;
        if(inParameters == null) {
            parameters = new Properties();
        } else {
            parameters = new Properties(inParameters);
        }
        if(inClasspath != null &&
           inClasspath.length != 0) {
            classpath = new String[inClasspath.length];
            System.arraycopy(inClasspath,
                             0,
                             classpath,
                             0,
                             inClasspath.length);
        } else {
            classpath = null;
        }
        outboundServicesProvider = inOutboundServicesProvider;
        inboundServicesProvider = inInboundServicesProvider;
        code = fileToString(getSource());
        defaultNamespace = inNamespace;
    }
    /**
     * Get the uniqueIdentifier value.
     *
     * @return a <code>String</code> value
     */
    final String getUniqueIdentifier()
    {
        return uniqueIdentifier;
    }
    /**
     * Get the source value.
     *
     * @return a <code>File</code> value
     */
    final File getSource()
    {
        return source;
    }
    /**
     * Get the executor value.
     *
     * @return an <code>Executor</code> value
     */
    final Executor getExecutor()
    {
        return executor;
    }
    /**
     * Get the runningStrategy value.
     *
     * @return a <code>RunningStrategy</code> value
     */
    final RunningStrategy getRunningStrategy()
    {
        return runningStrategy;
    }
    /**
     * Returns all currently running strategies. 
     *
     * @return a <code>Set&lt;StrategyImpl&gt;</code> value
     */
    static Set<StrategyImpl> getRunningStrategies()
    {
        synchronized(runningStrategies) {
            return new HashSet<StrategyImpl>(runningStrategies);
        }
    }
    /**
     * Sets the status of the strategy.
     *
     * @param inStatus a <code>Status</code> value
     */
    private void setStatus(Status inStatus)
    {
        status = inStatus;
        // update the running strategy collection
        if(status.equals(RUNNING)) {
            synchronized(runningStrategies) {
                runningStrategies.add(this);
            }
        } else {
            synchronized(runningStrategies) {
                runningStrategies.remove(this);
            }
        }
    }
    /**
     * Sets the runningStrategy value.
     *
     * @param a <code>RunningStrategy</code> value
     */
    private void setRunningStrategy(RunningStrategy inRunningStrategy)
    {
        runningStrategy = inRunningStrategy;
    }
    /**
     * Sets the executor value.
     *
     * @param an <code>Executor</code> value
     */
    private void setExecutor(Executor inExecutor)
    {
        executor = inExecutor;
    }
    /**
     * Reads the given <code>File</code> and renders its contents as a <code>String</code>.
     *
     * @param inFile a <code>File</code> value
     * @return a <code>String</code> value
     * @throws IOException if the <code>File</code> can not be read
     */
    private String fileToString(File inFile)
        throws IOException
    {
        return FileUtils.readFileToString(inFile);
    }
    /**
     * all strategies that are in RUNNING state 
     */
    private static final Set<StrategyImpl> runningStrategies = new HashSet<StrategyImpl>();
    /**
     * the user-applied name of the strategy.  this name has no strict correlation to any artifact declared by the embedded strategy itself.
     */
    final String name;
    /**
     * the type of the strategy being executed
     */
    private final Language language;
    /**
     * a reference to the actual code of the strategy
     */
    private final File source;
    /**
     * the actual code of the strategy
     */
    private final String code;
    /**
     * the set of parameters to pass to the strategy.  some of the values contained within may be meta-data that is relevant to the strategy manager (this object) rather than the strategy itself.
     */
    private final Properties parameters;
    /**
     * the classpath to pass to the strategy.  this may be null or empty.  if non-null, then the strategy executor will use the contents in a language-dependent way.
     */
    private final String[] classpath;
    /**
     * the provider of services for outgoing data via the strategy agent framework
     */
    private final OutboundServicesProvider outboundServicesProvider;
    /**
     * the provider of services for incoming data
     */
    private final InboundServicesProvider inboundServicesProvider;
    /**
     * the default namespace for this strategy
     */
    private final String defaultNamespace;
    /**
     * the value that uniquely identifies this strategy to the system within the scope of this JVM execution
     */
    private final String uniqueIdentifier;
    /**
     * the executor responsible for execution of this strategy
     */
    private Executor executor;
    /**
     * interface to the embedded running strategy object - this object is created by the execution engine
     */
    private RunningStrategy runningStrategy;
    /**
     * the strategy status
     */
    private Status status;
}
