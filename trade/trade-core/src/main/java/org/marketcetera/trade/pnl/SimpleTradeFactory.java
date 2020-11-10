//
// this file is automatically generated
//
package org.marketcetera.trade.pnl;

/* $License$ */

/**
 * Creates new {@link SimpleTrade} objects.
 *
 * @author <a href="mailto:colin@marketcetera.com">Colin DuPlantis</a>
 * @version $Id$
 * @since $Release$
 */
public class SimpleTradeFactory
        implements TradeFactory
{
    /**
     * Create a new <code>SimpleTrade</code> instance.
     *
     * @return a <code>SimpleTrade</code> value
     */
    @Override
    public SimpleTrade create()
    {
        return new SimpleTrade();
    }
    /**
     * Create a new <code>SimpleTrade</code> instance from the given object.
     *
     * @param inObject a <code>SimpleTrade</code> value
     * @return a <code>SimpleTrade</code> value
     */
    @Override
    public SimpleTrade create(Trade inSimpleTrade)
    {
        return new SimpleTrade(inSimpleTrade);
    }
}
