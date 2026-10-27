package com.game.shooting_game.ui;

import com.game.shooting_game.utils.Vector2;
import lombok.Data;

@Data
public class Anchor {
    public static final Anchor CENTER = new Anchor(H.CENTER, V.CENTER);
    public static final Anchor TOP_LEFT = new Anchor(H.LEFT, V.TOP);
    public static final Anchor TOP_CENTER = new Anchor(H.CENTER, V.TOP);
    public static final Anchor TOP_RIGHT = new Anchor(H.RIGHT, V.TOP);
    public static final Anchor CENTER_LEFT = new Anchor(H.LEFT, V.CENTER);
    public static final Anchor CENTER_RIGHT = new Anchor(H.RIGHT, V.CENTER);
    public static final Anchor BOTTOM_LEFT = new Anchor(H.LEFT, V.BOTTOM);
    public static final Anchor BOTTOM_CENTER = new Anchor(H.CENTER, V.BOTTOM);
    public static final Anchor BOTTOM_RIGHT = new Anchor(H.RIGHT, V.BOTTOM);

    public enum H {
        LEFT(0),
        CENTER(0.5),
        RIGHT(1);

        private final double rate;

        H(double rate) {
            this.rate = rate;
        }
    }

    public enum V {
        TOP(0),
        CENTER(0.5),
        BOTTOM(1);

        private final double rate;

        V(double rate) {
            this.rate = rate;
        }
    }

    public final H horizontal;
    public final V vertical;

    public Anchor(Anchor anchor) {
        this.horizontal = anchor.horizontal;
        this.vertical = anchor.vertical;
    }

    public Anchor(H h, V v) {
        this.horizontal = h;
        this.vertical = v;
    }

    public Vector2 getValue() {
        return new Vector2(horizontal.rate, vertical.rate);
    }
}
