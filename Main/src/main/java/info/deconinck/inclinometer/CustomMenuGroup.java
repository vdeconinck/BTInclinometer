package info.deconinck.inclinometer;

import java.util.List;

/**
 * Created by 葛文博 on 2017/11/21.
 */
public class CustomMenuGroup {
    private String name;
    private List<CustomMenuItem> childList;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<CustomMenuItem> getChildList() {
        return childList;
    }

    public void setChildList(List<CustomMenuItem> childList) {
        this.childList = childList;
    }
}
