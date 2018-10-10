package org.hwyl.sexytopo.comms;

import org.junit.Assert;
import org.junit.Test;


public class DistoXProtocolTest {

    private static final byte i = 0b00000000;

    @Test
    public void testAcknowledgePacketMatchesByteEndingIn1() {
        byte control = 0b00000011;
        byte[] packet = new byte[]{control, i, i, i, i, i, i, i};
        byte actual = DistoXProtocol.createAcknowledgementPacket(packet)[0];
        Assert.assertEquals((byte)0b10101011, actual);
    }

    @Test
    public void testAcknowledgePacketMatchesByteEndingIn0() {
        byte control = 0b00000010;
        byte[] packet = new byte[]{control, i, i, i, i, i, i, i};
        byte actual = DistoXProtocol.createAcknowledgementPacket(packet)[0];
        Assert.assertEquals((byte)0b10101010, actual);
    }

}
