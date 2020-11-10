//
// this file is automatically generated
//
package org.marketcetera.trade.pnl.dao;

/* $License$ */

/**
 * Provides a user-centric view of a {@link Trade}.
 *
 * @author <a href="mailto:colin@marketcetera.com">Colin DuPlantis</a>
 * @version $Id$
 * @since $Release$
 */
@javax.persistence.Entity(name="UserTrade")
@javax.persistence.Table(name="user_trades")
public class PersistentUserTrade
        extends org.marketcetera.persist.EntityBase
        implements org.marketcetera.trade.pnl.UserTrade
{
    /**
     * Create a new PersistentUserTrade instance.
     */
    public PersistentUserTrade() {}
    /**
     * Create a new PersistentUserTrade instance.
     *
     * @param inUserTrade a <code>UserTrade</code> value
     */
    public PersistentUserTrade(org.marketcetera.trade.pnl.UserTrade inUserTrade)
    {
        setTrade(inUserTrade.getTrade());
        setUser(inUserTrade.getUser());
        setSide(inUserTrade.getSide());
        setProfitAndLoss(inUserTrade.getProfitAndLoss());
        setOrderId(inUserTrade.getOrderId());
    }
    /**
     * Get the trade value.
     *
     * @return a <code>Trade</code> value
     */
    @Override
    public org.marketcetera.trade.pnl.Trade getTrade()
    {
        return trade;
    }
    /**
     * Set the trade value.
     *
     * @param inTrade a <code>Trade</code> value
     */
    @Override
    public void setTrade(org.marketcetera.trade.pnl.Trade inTrade)
    {
        trade = (PersistentTrade)inTrade;
    }
    /**
     * Get the user value.
     *
     * @return a <code>org.marketcetera.admin.User</code> value
     */
    @Override
    public org.marketcetera.admin.User getUser()
    {
        return user;
    }
    /**
     * Set the user value.
     *
     * @param inUser a <code>org.marketcetera.admin.User</code> value
     */
    @Override
    public void setUser(org.marketcetera.admin.User inUser)
    {
        user = (org.marketcetera.admin.user.PersistentUser)inUser;
    }
    /**
     * Get the side value.
     *
     * @return a <code>org.marketcetera.trade.Side</code> value
     */
    @Override
    public org.marketcetera.trade.Side getSide()
    {
        return side;
    }
    /**
     * Set the side value.
     *
     * @param inSide a <code>org.marketcetera.trade.Side</code> value
     */
    @Override
    public void setSide(org.marketcetera.trade.Side inSide)
    {
        side = inSide;
    }
    /**
     * Get the profitAndLoss value.
     *
     * @return a <code>ProfitAndLoss</code> value
     */
    @Override
    public org.marketcetera.trade.pnl.ProfitAndLoss getProfitAndLoss()
    {
        return profitAndLoss;
    }
    /**
     * Set the profitAndLoss value.
     *
     * @param inProfitAndLoss a <code>ProfitAndLoss</code> value
     */
    @Override
    public void setProfitAndLoss(org.marketcetera.trade.pnl.ProfitAndLoss inProfitAndLoss)
    {
        profitAndLoss = (PersistentProfitAndLoss)inProfitAndLoss;
    }
    /**
     * Get the orderId value.
     *
     * @return a <code>org.marketcetera.trade.OrderID</code> value
     */
    @Override
    public org.marketcetera.trade.OrderID getOrderId()
    {
        return orderId;
    }
    /**
     * Set the orderId value.
     *
     * @param inOrderId a <code>org.marketcetera.trade.OrderID</code> value
     */
    @Override
    public void setOrderId(org.marketcetera.trade.OrderID inOrderId)
    {
        orderId = inOrderId;
    }
    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder();
        builder.append("UserTrade [")
            .append("trade=").append(trade)
            .append(", user=").append(user)
            .append(", side=").append(side)
            .append(", profitAndLoss=").append(profitAndLoss)
            .append(", orderId=").append(orderId).append("]");
        return builder.toString();
    }
    /**
     * underlying trade value
     */
    @javax.persistence.ManyToOne(fetch=javax.persistence.FetchType.EAGER,optional=false)
    @javax.persistence.JoinColumn(name="trade_id",nullable=false)
    private PersistentTrade trade;
    /**
     * user which owns lot
     */
    @javax.persistence.ManyToOne(fetch=javax.persistence.FetchType.EAGER,optional=false)
    @javax.persistence.JoinColumn(name="user_id",nullable=false)
    private org.marketcetera.admin.user.PersistentUser user;
    /**
     * side of this trade
     */
    @javax.persistence.Column(name="side",nullable=false)
    private org.marketcetera.trade.Side side;
    /**
     * profit and loss data for this trade
     */
    @javax.persistence.OneToOne(fetch=javax.persistence.FetchType.EAGER,optional=false)
    @javax.persistence.JoinColumn(name="pnl_id",nullable=false)
    private PersistentProfitAndLoss profitAndLoss;
    /**
     * order ID for this trade
     */
    @javax.persistence.Column(name="order_id",nullable=false)
    private org.marketcetera.trade.OrderID orderId;
    private static final long serialVersionUID = -17885022342939L;
}
