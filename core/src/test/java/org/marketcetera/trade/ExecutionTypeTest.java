package org.marketcetera.trade;

import static org.junit.Assert.assertEquals;
import static org.marketcetera.trade.ExecutionType.Calculated;
import static org.marketcetera.trade.ExecutionType.Canceled;
import static org.marketcetera.trade.ExecutionType.DoneForDay;
import static org.marketcetera.trade.ExecutionType.Expired;
import static org.marketcetera.trade.ExecutionType.Fill;
import static org.marketcetera.trade.ExecutionType.New;
import static org.marketcetera.trade.ExecutionType.OrderStatus;
import static org.marketcetera.trade.ExecutionType.PartialFill;
import static org.marketcetera.trade.ExecutionType.PendingCancel;
import static org.marketcetera.trade.ExecutionType.PendingNew;
import static org.marketcetera.trade.ExecutionType.PendingReplace;
import static org.marketcetera.trade.ExecutionType.Rejected;
import static org.marketcetera.trade.ExecutionType.Replaced;
import static org.marketcetera.trade.ExecutionType.Restated;
import static org.marketcetera.trade.ExecutionType.Stopped;
import static org.marketcetera.trade.ExecutionType.Suspended;
import static org.marketcetera.trade.ExecutionType.Trade;
import static org.marketcetera.trade.ExecutionType.TradeCancel;
import static org.marketcetera.trade.ExecutionType.TradeCorrect;
import static org.marketcetera.trade.ExecutionType.Unknown;
import static org.marketcetera.trade.ExecutionType.values;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;
import org.marketcetera.core.Pair;

import quickfix.field.ExecType;

/* $License$ */
/**
 * Tests {@link ExecutionType}
 *
 * @author anshul@marketcetera.com
 * @version $Id$
 * @since 1.0.0
 */
public class ExecutionTypeTest extends FIXCharEnumTestBase<ExecutionType> {
    /**
     * Tests {@link ExecutionType#isFill()}.
     *
     * @throws Exception if an unexpected error occurs
     */
    @Test
    public void testIsFill()
            throws Exception
    {
        Set<ExecutionType> expectedFills = EnumSet.of(Fill,Trade,PartialFill,Restated,TradeCorrect);
        for(ExecutionType type : values()) {
            assertEquals(expectedFills.contains(type),type.isFill());
        }
    }
    @Override
    protected ExecutionType getInstanceForFIXValue(Character inFIXValue) {
        return ExecutionType.getInstanceForFIXValue(inFIXValue);
    }

    @Override
    protected Character getFIXValue(ExecutionType e) {
        return e.getFIXValue();
    }

    @Override
    protected ExecutionType unknownInstance() {
        return Unknown;
    }

    @Override
    protected List<ExecutionType> getValues() {
        return Arrays.asList(ExecutionType.values());
    }

    @Override
    protected List<Pair<ExecutionType,Character>> knownValues() {
        return Arrays.asList(
                new Pair<ExecutionType, Character>(New, ExecType.NEW),
                new Pair<ExecutionType, Character>(PartialFill, ExecType.PARTIAL_FILL),
                new Pair<ExecutionType, Character>(Fill, ExecType.FILL),
                new Pair<ExecutionType, Character>(DoneForDay, ExecType.DONE_FOR_DAY),
                new Pair<ExecutionType, Character>(Canceled, ExecType.CANCELED),
                new Pair<ExecutionType, Character>(Replaced, ExecType.REPLACED),
                new Pair<ExecutionType, Character>(PendingCancel, ExecType.PENDING_CANCEL),
                new Pair<ExecutionType, Character>(Stopped, ExecType.STOPPED),
                new Pair<ExecutionType, Character>(Rejected, ExecType.REJECTED),
                new Pair<ExecutionType, Character>(Suspended, ExecType.SUSPENDED),
                new Pair<ExecutionType, Character>(PendingNew, ExecType.PENDING_NEW),
                new Pair<ExecutionType, Character>(Calculated, ExecType.CALCULATED),
                new Pair<ExecutionType, Character>(Expired, ExecType.EXPIRED),
                new Pair<ExecutionType, Character>(Restated, ExecType.RESTATED),
                new Pair<ExecutionType, Character>(PendingReplace, ExecType.PENDING_REPLACE),
                new Pair<ExecutionType, Character>(Trade, ExecType.TRADE),
                new Pair<ExecutionType, Character>(TradeCorrect, ExecType.TRADE_CORRECT),
                new Pair<ExecutionType, Character>(TradeCancel, ExecType.TRADE_CANCEL),
                new Pair<ExecutionType, Character>(OrderStatus, ExecType.ORDER_STATUS)
        );
    }
}
