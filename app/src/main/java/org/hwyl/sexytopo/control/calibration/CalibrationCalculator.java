package org.hwyl.sexytopo.control.calibration;

import org.hwyl.sexytopo.control.calibration.topodroid.CalibAlgo;
import org.hwyl.sexytopo.control.calibration.topodroid.CalibAlgoBH;
import org.hwyl.sexytopo.control.calibration.topodroid.CalibCBlock;
import org.hwyl.sexytopo.control.calibration.topodroid.TDMath;
import org.hwyl.sexytopo.model.calibration.CalibrationReading;

import java.util.ArrayList;
import java.util.List;


public class CalibrationCalculator {

    private CalibAlgo algorithm;

    public int calculate(List<CalibrationReading> calibrationReadings) {


        List<CalibCBlock> blocks = createCalibCBlockList(calibrationReadings);
        computeGroups(blocks);

        //GroupReadings.group(blocks, -1);

        algorithm = new CalibAlgoBH(0, false); // FIXME should this be FALSE?

        for (CalibCBlock block : blocks) {
            algorithm.AddValues(block);
        }

        return algorithm.Calibrate();
    }


    public void computeGroupsByDistance(List<CalibCBlock> blocks) {
        final float GROUP_DISTANCE = 40;
        long group = 0;
        float b = 0.0f;
        float c = 0.0f;
        float thr = TDMath.cosd(GROUP_DISTANCE);
        for (CalibCBlock block : blocks) {
            if (group == 0 || block.isFarFrom(b, c, thr)) {
                ++group;
                b = block.mBearing;
                c = block.mClino;
            }
            block.setGroup(group);
        }
    }

    public void computeGroupsByFour(List<CalibCBlock> list) {
        final float GROUP_DISTANCE = 40;
        long group = 0;
        float b = 0.0f;
        float c = 0.0f;
        int cnt = 0;

        for (CalibCBlock item : list) {
            //if ( start_id >= 0 && item.mId <= start_id ) continue;
            item.setGroupIfNonZero(group);
            //mApp_mDData.updateGMName( item.mId, item.mCalibId, Long.toString( item.mGroup ) );
            ++cnt;
            if ((cnt % 4) == 0) {
                ++group;
                // TDLog.Log( TDLog.LOG_CALIB, "cnt " + cnt + " new group " + group );
            }
        }
    }


    public double getMaxError() {
        return algorithm.MaxError();
    }


    public byte[] getCoefficients() {
        return algorithm.getCoeff();
    }


    private static List<CalibCBlock> createCalibCBlockList(List<CalibrationReading> readings) {
        List<CalibCBlock> blocks = new ArrayList<>();
        for (CalibrationReading reading : readings) {
            blocks.add(createCalibCBlock(reading));
        }
        return blocks;
    }

    private static CalibCBlock createCalibCBlock (CalibrationReading reading) {
        CalibCBlock block = new CalibCBlock();
        block.setData(
            reading.getGx(),
            reading.getGy(),
            reading.getGz(),
            reading.getMx(),
            reading.getMy(),
            reading.getMz()
        );
        return block;
    }

}