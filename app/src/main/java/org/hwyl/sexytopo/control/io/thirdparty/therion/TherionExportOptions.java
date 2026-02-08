package org.hwyl.sexytopo.control.io.thirdparty.therion;

/**
 * Options for Therion export, typically set via a dialog before export.
 */
public class TherionExportOptions {

    private int planScrapCount = 1;
    private int eeScrapCount = 1;
    private boolean stationsInFirstPlanScrap = true;
    private boolean stationsInFirstEeScrap = true;

    public TherionExportOptions() {
    }

    public TherionExportOptions(int planScrapCount, int eeScrapCount,
                                boolean stationsInFirstPlanScrap, boolean stationsInFirstEeScrap) {
        this.planScrapCount = planScrapCount;
        this.eeScrapCount = eeScrapCount;
        this.stationsInFirstPlanScrap = stationsInFirstPlanScrap;
        this.stationsInFirstEeScrap = stationsInFirstEeScrap;
    }

    public int getPlanScrapCount() {
        return planScrapCount;
    }

    public void setPlanScrapCount(int planScrapCount) {
        this.planScrapCount = Math.max(1, planScrapCount);
    }

    public int getEeScrapCount() {
        return eeScrapCount;
    }

    public void setEeScrapCount(int eeScrapCount) {
        this.eeScrapCount = Math.max(1, eeScrapCount);
    }

    public boolean isStationsInFirstPlanScrap() {
        return stationsInFirstPlanScrap;
    }

    public void setStationsInFirstPlanScrap(boolean stationsInFirstPlanScrap) {
        this.stationsInFirstPlanScrap = stationsInFirstPlanScrap;
    }

    public boolean isStationsInFirstEeScrap() {
        return stationsInFirstEeScrap;
    }

    public void setStationsInFirstEeScrap(boolean stationsInFirstEeScrap) {
        this.stationsInFirstEeScrap = stationsInFirstEeScrap;
    }
}
