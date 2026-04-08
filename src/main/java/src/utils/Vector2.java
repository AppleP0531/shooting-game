package src.utils;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 二次元ベクトルクラス
 */
@Data // Getter, Setterの自動生成
@NoArgsConstructor // 引数なしコンストラクタの生成
@AllArgsConstructor // 全引数コンストラクタの生成
@JsonPropertyOrder({ "x", "y" }) // 並び順の指定
@JsonFormat(shape = JsonFormat.Shape.ARRAY) // 配列として扱えるようにする
public class Vector2 {
    private double x, y;

    public Vector2(Vector2 vec) {
        this.x = vec.getX();
        this.y = vec.getY();
    }
}