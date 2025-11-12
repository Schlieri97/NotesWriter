package com.code.noteswriter;

import java.util.HashMap;
import java.util.Map;

public class Model {
    private final Map<String, boolean[]> griffTabellen;

    public Model() {
        griffTabellen = new HashMap<>();

        griffTabellen.put("D_Oktav", new boolean[] {true, true, true, true, true, true, true});
        griffTabellen.put("Dis_Es_Oktav", new boolean[] {true, true, true, true, true, true, false});
        griffTabellen.put("E_Oktav", new boolean[] {true, true, true, true, true, false, false});
        griffTabellen.put("F_Oktav", new boolean[] {true, true, true, true, false, false, false});
        griffTabellen.put("G_Oktav", new boolean[] {true, true, true, false, false, false, false});
        griffTabellen.put("A_Oktav", new boolean[] {true, true, false, false, false, false, false});
        griffTabellen.put("H_Oktav", new boolean[] {true, false, false, false, false, false, false});
        griffTabellen.put("C_Oktav", new boolean[] {false, true, true, false, false, false, false});

        griffTabellen.put("Cis_Des", new boolean[] {false, false, false, false, false, false, false});
        griffTabellen.put("D", new boolean[] {false, true, true, true, true, true, true});
        griffTabellen.put("Dis_Es", new boolean[] {true, true, true, true, true, true, false});
        griffTabellen.put("E", new boolean[] {true, true, true, true, true, false, false});
        griffTabellen.put("F", new boolean[] {true, true, true, true, false, false, false});
        griffTabellen.put("F_1", new boolean[] {true, true, true, true, false, true, false});
        griffTabellen.put("Fis_Ges", new boolean[] {true, true, true, false, true, true, false});
        griffTabellen.put("G", new boolean[] {true, true, true, false, false, false, false});
        griffTabellen.put("Gis_As", new boolean[] {true, true, false, true, false, false, false});
        griffTabellen.put("A", new boolean[] {true, true, false, false, false, false, false});
        griffTabellen.put("Ais_B", new boolean[] {true, true, false, true, true, true, false});
        griffTabellen.put("H", new boolean[] {true, false, false, false, false, false, false});
        griffTabellen.put("C1", new boolean[] {false, true, false, true, true, true, false});
        griffTabellen.put("Cis_Des1", new boolean[] {false, true, true, true, false, false, false});
        griffTabellen.put("D1", new boolean[] {false, true, true, false, false, false, false});
        griffTabellen.put("Dis_Es1", new boolean[] {false, true, true, false, true, true, false});
        griffTabellen.put("E1", new boolean[] {true, true, false, false, true, true, false});
        griffTabellen.put("Fis_Ges1", new boolean[] {true, false, false, true, false, false, true});
        griffTabellen.put("G1", new boolean[] {true, false, true, false, false, false, true});
        griffTabellen.put("Gis_As1", new boolean[] {false, false, true, false, false, true, false});
        griffTabellen.put("A1", new boolean[] {false, true, true, true, true, false, true});

        griffTabellen.put("W_Offen", new boolean[] {false, false, false, false, false, false, false});
        griffTabellen.put("W_Zu", new boolean[] {false, false, false, false, false, false, false});
    }

    public boolean[] getGriff(String note) {
        return griffTabellen.getOrDefault(note, new boolean[7]);
    }
}
