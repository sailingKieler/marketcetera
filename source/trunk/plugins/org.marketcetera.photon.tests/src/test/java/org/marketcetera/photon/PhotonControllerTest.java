package org.marketcetera.photon;

import junit.framework.Test;
import junit.framework.TestCase;
import org.marketcetera.core.ClassVersion;
import org.marketcetera.core.InMemoryIDFactory;
import org.marketcetera.core.MSymbol;
import org.marketcetera.core.MarketceteraTestSuite;
import org.marketcetera.photon.core.FIXMessageHistory;
import org.marketcetera.quickfix.FIXMessageFactory;
import org.marketcetera.quickfix.FIXVersion;
import quickfix.Message;
import quickfix.field.*;

import java.math.BigDecimal;
import java.util.Vector;

/**
 * Verify the functions in PhotonController
 * @author toli
 * @version $Id$
 */

@ClassVersion("$Id$")
public class PhotonControllerTest extends TestCase {
    private static FIXMessageFactory msgFactory = FIXVersion.FIX42.getMessageFactory();
    private MyPhotonController photonController;
    private FIXMessageHistory fixMessageHistory;

    public PhotonControllerTest(String inName) {
        super(inName);
    }

    protected void setUp() throws Exception {
        super.setUp();
        photonController = new MyPhotonController();
        fixMessageHistory = new FIXMessageHistory(msgFactory);
        photonController.setMessageHistory(fixMessageHistory);
        photonController.setIDFactory(new InMemoryIDFactory(1000));
        photonController.setMessageFactory(msgFactory);
    }

    public void testCancelAllOpenOrders() throws Exception {
        fixMessageHistory.addIncomingMessage(msgFactory.newExecutionReport("123", "10001", "201", OrdStatus.NEW,
                Side.BUY, new BigDecimal(10), new BigDecimal(10.10), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, new MSymbol("XYZ"), "tester"));
        fixMessageHistory.addIncomingMessage(msgFactory.newExecutionReport("123", "10002", "201", OrdStatus.NEW,
                Side.BUY, new BigDecimal(10), new BigDecimal(10.10), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, new MSymbol("BOB"), "tester"));
        photonController.cancelAllOpenOrders();
        assertEquals("not enough orders canceled", 2, photonController.sentMessages.size());
        assertEquals(MsgType.ORDER_CANCEL_REQUEST, photonController.sentMessages.get(0).getHeader().getString(MsgType.FIELD));
        assertEquals(MsgType.ORDER_CANCEL_REQUEST, photonController.sentMessages.get(1).getHeader().getString(MsgType.FIELD));
        assertEquals("10001", photonController.sentMessages.get(0).getString(OrigClOrdID.FIELD));
        assertEquals("10002", photonController.sentMessages.get(1).getString(OrigClOrdID.FIELD));
    }

    /** Store the messages that are meant to go out */
    private class MyPhotonController extends PhotonController {
        private Vector<Message> sentMessages = new Vector<Message>();
        protected void convertAndSend(Message fixMessage) {
            sentMessages.add(fixMessage);
        }
    }
}