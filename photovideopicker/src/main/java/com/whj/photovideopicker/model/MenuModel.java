package com.whj.photovideopicker.model;

/**
 * @author William
 * @Github WHuaJian
 * Created at 2018/5/11 下午2:24
 */

public class MenuModel {

    private int tabId;
    private String tabName;
    private boolean clicked;

    public MenuModel() {
    }

    public MenuModel(int tabId, String tabName, boolean clicked) {
        this.tabId = tabId;
        this.tabName = tabName;
        this.clicked = clicked;
    }

    public int getTabId() {
        return tabId;
    }

    public void setTabId(int tabId) {
        this.tabId = tabId;
    }

    public String getTabName() {
        return tabName;
    }

    public void setTabName(String tabName) {
        this.tabName = tabName;
    }

    public boolean isClicked() {
        return clicked;
    }

    public void setClicked(boolean clicked) {
        this.clicked = clicked;
    }
}
