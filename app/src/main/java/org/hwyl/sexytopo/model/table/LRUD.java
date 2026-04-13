package org.hwyl.sexytopo.model.table;

import org.hwyl.sexytopo.control.Log;
import org.hwyl.sexytopo.control.util.CrossSectioner;
import org.hwyl.sexytopo.control.util.Space2DUtils;
import org.hwyl.sexytopo.model.survey.Leg;
import org.hwyl.sexytopo.model.survey.Station;
import org.hwyl.sexytopo.model.survey.Survey;

/**
 * Handles LRUD splays, a convenience provided for users of the manual entry feature. LRUDs (Left,
 * Right, Up, Down) are splays that are calculated/estimated from only the distance given.
 */
public enum LRUD {
    LEFT {
        public Leg createSplay(Survey survey, Station station, Mode mode, float distance) {
            float angle = Space2DUtils.adjustAngle(mode.getSideAzimuth(survey, station), -90.0f);
            return new Leg(distance, angle, 0);
        }
    },
    RIGHT {
        public Leg createSplay(Survey survey, Station station, Mode mode, float distance) {
            float angle = Space2DUtils.adjustAngle(mode.getSideAzimuth(survey, station), 90.0f);
            return new Leg(distance, angle, 0);
        }
    },
    UP {
        public Leg createSplay(Survey survey, Station station, Mode mode, float distance) {
            return new Leg(distance, 0, 90.0f);
        }
    },
    DOWN {
        public Leg createSplay(Survey survey, Station station, Mode mode, float distance) {
            return new Leg(distance, 0, -90.0f);
        }
    };

    public enum Mode {
        /* Names here are taken from Therion usage */
        SURVEY {
            public float getSideAzimuth(Survey survey, Station station) {
                return CrossSectioner.getAngleOfSection(survey, station);
            }
        },
        SHOT {
            public float getSideAzimuth(Survey survey, Station station) {
                return station.getConnectedOnwardLegs().get(0).getAzimuth();
            }
        };

        public abstract float getSideAzimuth(Survey survey, Station station);

        public static Mode fromPreferenceValue(String value) {
            try {
                return valueOf(value.toUpperCase());
            } catch (IllegalArgumentException e) {
                Log.e("Unknown LRUD mode: " + value + "; defaulting to SURVEY");
                return SURVEY;
            }
        }
    }

    public abstract Leg createSplay(Survey survey, Station station, Mode mode, float distance);
}
