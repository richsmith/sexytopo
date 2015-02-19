package org.hwyl.sexytopo.comms;

import org.hwyl.sexytopo.model.survey.Leg;
import org.hwyl.sexytopo.model.survey.Survey;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by rls on 16/07/14.
 */
public class Communicator extends Thread {

    private int POLLING_FREQUENCY = 10 * 1000;

    private Survey survey;
    private DistoXProtocolOld protocol;

    public Communicator(Survey survey,
                        DistoXProtocolOld protocol) {
        this.survey = survey;
        this.protocol = protocol;
    }

    public void run() {

        while(true) {
            try {
                sleep(POLLING_FREQUENCY);
                List<Leg> legs = slurpAvailableData(protocol);
                //survey.addLegs(legs);
                int ingore = 0;
            } catch (Exception e) {
                int ignore = 0;
            }
        }
    }

    public List<Leg> slurpAvailableData(DistoXProtocolOld protocol) {

        List<Leg> legs = new ArrayList<>();

        loop:
        while (true) {
            int toRead = protocol.readToRead();
            int result = protocol.readPacket();
            switch (result) {
                case DistoXProtocolOld.DISTOX_PACKET_NONE:
                    break loop;
                case DistoXProtocolOld.DISTOX_PACKET_DATA:
                    legs.add(getLeg(protocol));
                    break;
                default:
                    int ignore = 0;
            }
        }

        return legs;
    }

    private Leg getLeg(DistoXProtocolOld protocol) {
        double distance = protocol.Distance();
        double compass = protocol.Compass();
        double clinometer = protocol.Clino();
        /*
              double r = mProto.Roll();
              */

        Leg leg = new Leg(distance, compass, clinometer);
        return leg;

    }

}
