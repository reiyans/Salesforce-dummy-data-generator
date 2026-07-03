import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Salesforce Inspector ReloadedのShow all画面からコピーしたテキストを解析するクラス。
 * 「API参照名」「表示ラベル」「データ型」「値」が縦に4行1セットで繰り返される形式を前提とする。
 *
 * 既知の制約:「値」列が空の項目があると4行の並びが崩れてズレる。
 * 現時点では対策せず、貼り付け前の目視確認で運用回避する。
 */
public class ShowAllParser {

    private static final Set<String> HEADER_FIRST_LINE_TOKENS = new HashSet<>(Arrays.asList(
            "api参照名", "api name", "api reference name", "フィールド名"
    ));

    public static List<FieldDefinition> parse(String pastedText) {
        List<FieldDefinition> result = new ArrayList<>();
        if (pastedText == null || pastedText.isBlank()) {
            return result;
        }

        List<String> lines = new ArrayList<>(Arrays.asList(pastedText.split("\r?\n", -1)));

        // 貼り付け時に混入しがちな先頭・末尾の空行だけを取り除く(中間の空行はそのまま残す)
        while (!lines.isEmpty() && lines.get(0).trim().isEmpty()) {
            lines.remove(0);
        }
        while (!lines.isEmpty() && lines.get(lines.size() - 1).trim().isEmpty()) {
            lines.remove(lines.size() - 1);
        }

        int startIndex = 0;
        if (!lines.isEmpty() && HEADER_FIRST_LINE_TOKENS.contains(lines.get(0).trim().toLowerCase())) {
            startIndex = 4;
        }

        for (int i = startIndex; i + 3 < lines.size(); i += 4) {
            String apiName = lines.get(i).trim();
            String label = lines.get(i + 1).trim();
            String type = lines.get(i + 2).trim();
            String value = lines.get(i + 3).trim();
            result.add(new FieldDefinition(apiName, label, type, value));
        }

        return result;
    }
}
