package org.example.info;

import org.example.graph.GraphEvent;

public class InfoUtil {
    private static Info info;

    public static void setInfo(Info info) {
        InfoUtil.info = info;
    }

    public static void updateCycleLog(GraphEvent ev)  {
        info.updateGraphInfo(ev);
    }
}
