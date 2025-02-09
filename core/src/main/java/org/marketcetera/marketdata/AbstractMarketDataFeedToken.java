package org.marketcetera.marketdata;

import java.util.concurrent.ExecutionException;

import org.marketcetera.core.publisher.Subscriber;
import org.marketcetera.core.publisher.PublisherEngine;

/**
 * Represents the responses to a market data request.
 *
 * <p>This class provides a handle to the responses to a market data request.  A token
 * has a stateless and a stateful component.  The stateless component, the
 * <code>MarketDataFeedTokenSpec</code> contains the information necessary to reproduce
 * the query represented by this token.
 * 
 * @author <a href="mailto:colin@marketcetera.com">Colin DuPlantis</a>
 * @version $Id$
 * @since 0.5.0
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
public abstract class AbstractMarketDataFeedToken<F extends AbstractMarketDataFeed>
    implements MarketDataFeedToken
{
    /**
     * the stateless portion of the token which represents the original request 
     */
    private final MarketDataFeedTokenSpec mTokenSpec;
    /**
     * the data feed to which this token is bound
     */
    private final F mFeed;
    /**
     * publishing engine used to update subscribers of responses to the request
     */
    private final PublisherEngine mPublisher;        
    /**
     * the status of this token
     */
    private Status mStatus = Status.NOT_STARTED;
    /**
     * Create a new <code>AbstractMarketDataFeedToken</code> object.
     *
     * @param inTokenSpec a <code>MarketDataFeedTokenSpec&lt;C&gt;</code> value encapsulating the data feed request
     * @param inFeed a <code>F</code> value containing the feed to which this token is bound
     * @throws NullPointerException if the token spec or feed is null
     */
    protected AbstractMarketDataFeedToken(MarketDataFeedTokenSpec inTokenSpec,
                                          F inFeed) 
    {
        if(inTokenSpec == null ||
           inFeed == null) {
            throw new NullPointerException();
        }
        mTokenSpec = inTokenSpec;
        mFeed = inFeed;
        mPublisher = new PublisherEngine(true);
    }
    /**
     * Publishes the given data to all subscribers.
     *
     * @param inData an <code>Object</code> value
     * @throws InterruptedException if synchronous publications are selected and the thread is interrupted
     *   while notifying a publisher
     * @throws ExecutionException 
     */
    @Override
    public final void publish(Object inData) 
    {
        getPublisher().publish(inData);
    }
    /* (non-Javadoc)
     * @see org.marketcetera.core.publisher.IPublisher#publishAndWait(java.lang.Object)
     */
    @Override
    public final void publishAndWait(Object inData)
            throws InterruptedException, ExecutionException
    {
        getPublisher().publish(inData);
    }
    /**
     * Adds all given subscribers to the subscriber list.
     *
     * @param inSubscribers an <code>Subscriber...</code> value
     */
    public final void subscribeAll(Subscriber... inSubscribers)
    {
        if(inSubscribers == null) {
            return;
        }
        for(Subscriber subscriber : inSubscribers) {
            subscribe(subscriber);
        }
    }
    /* (non-Javadoc)
     * @see org.marketcetera.core.publisher.IPublisher#subscribe(org.marketcetera.core.publisher.Subscriber)
     */
    public final void subscribe(Subscriber inSubscriber)
    {
        getPublisher().subscribe(inSubscriber);
    }
    /* (non-Javadoc)
     * @see org.marketcetera.core.publisher.IPublisher#unsubscribe(org.marketcetera.core.publisher.Subscriber)
     */
    public final void unsubscribe(Subscriber inSubscriber)
    {
        getPublisher().unsubscribe(inSubscriber);
    }
    /**
     * Get the status value.
     *
     * @return a <code>Status</code> value
     */
    public final Status getStatus()
    {
        return mStatus;
    }
    /**
     * Sets the status value.
     *
     * @param inStatus a <code>AbstractMarketDataFeedToken</code> value
     */
    protected final void setStatus(Status inStatus)
    {
        mStatus = inStatus;
    }
    /* (non-Javadoc)
     * @see org.marketcetera.marketdata.IMarketDataFeedToken#cancel()
     */
    // this is to avoid having to add all the types to the class declaration to
    //  define the type of mFeed
    public final void cancel()
    {
        if(getStatus().cancelable()) {
            mFeed.cancel(this);
        }
    }
    /* (non-Javadoc)
     * @see org.marketcetera.marketdata.IMarketDataFeedToken#getTokenSpec()
     */
    public final MarketDataFeedTokenSpec getTokenSpec()
    {
        return mTokenSpec;
    }    
    /**
     * Gets the publisher engine.
     * 
     * @return a <code>PublisherEnginer</code> value
     */
    final PublisherEngine getPublisher()
    {
        return mPublisher;
    }
    /* (non-Javadoc)
     * @see org.marketcetera.core.publisher.IPublisher#getSubscriptionCount()
     */
    @Override
    public int getSubscriptionCount()
    {
        return mPublisher.getSubscriptionCount();
    }
}
