package org.hwyl.sexytopo.comms;

import org.hwyl.sexytopo.comms.distoxble.DistoXBleManager;
import org.junit.Assert;
import org.junit.Test;

public class DistoXBleManagerTest {

    @Test
    public void testCreateWritePacket() {
        Byte command = 0x30;
        Byte[] packet = DistoXBleManager.createWriteCommandPacket(command);
        Assert.assertEquals(9, packet.length);

    }

}