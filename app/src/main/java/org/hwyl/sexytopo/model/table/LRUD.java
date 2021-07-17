package org.hwyl.sexytopo.model.table;

import org.hwyl.sexytopo.control.util.CrossSectioner;
import org.hwyl.sexytopo.control.util.Space2DUtils;
import org.hwyl.sexytopo.model.survey.Leg;
import org.hwyl.sexytopo.model.survey.Station;
import org.hwyl.sexytopo.model.survey.Survey;

/**
 * Handles LRUD splays, a convenience provided for users of the manual entry feature. LRUDs
 * (Left, Right, Up, Down) are splays that are calculated/estimated from only the distance given.
 */
public enum LRUD {
    LEFT {
        public Leg createSplay(Survey survey, Station station, float distance) {
            float angle = Space2DUtils.adjustAngle(
                    CrossSectioner.getAngleOfSection(survey, station),
                    -90.0f);
            return new Leg(distance, angle, 0);
        }
    },
    RIGHT {
        public Leg createSplay(Survey survey, Station station, float distance) {
            float angle = Space2DUtils.adjustAngle(
                    CrossSectioner.getAngleOfSection(survey, station),
                    90.0f);
            return new Leg(distance, angle, 0);
        }
    },
    UP {
        public Leg createSplay(Survey survey, Station station, float distance) {
            return new Leg(distance, 0, 90.0f);
        }
    },
    DOWN {
        public Leg createSplay(Survey survey, Station station, float distance) {
            return new Leg(distance, 0, -90.0f);
        }
    };

    public abstract Leg createSplay(Survey survey, Station station, float distance);
}
