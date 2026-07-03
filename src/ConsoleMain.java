import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

/**
 * GUIなしのコンソール版。パース処理とダミー生成ロジックの動作確認用。
 * Salesforce Inspector ReloadedのShow all画面からのコピー結果を模したサンプルテキストを使う。
 * ※すべて検証用トライアル組織を想定したダミー項目・ダミー値であり、実データは含まない。
 */
public class ConsoleMain {

    private static final String SAMPLE_PASTED_TEXT = String.join("\n",
            "API参照名",
            "表示ラベル",
            "データ型",
            "値",
            "Name",
            "取引先名",
            "text (80),required",
            "テスト株式会社",
            "AccountNumber",
            "取引先番号",
            "text (40)",
            "ACC-0001",
            "Industry",
            "業種",
            "picklist",
            "Technology",
            "AnnualRevenue",
            "年間売上高",
            "currency (16, 2)",
            "1000000",
            "NumberOfEmployees",
            "従業員数",
            "number (8, 0)",
            "50",
            "Active__c",
            "有効",
            "checkbox",
            "true",
            "LastActivityDate",
            "最終活動日",
            "date",
            "2024-01-01",
            "CreatedDate",
            "作成日時",
            "datetime",
            "2024-01-01T10:00:00.000Z",
            "PersonEmail",
            "メールアドレス",
            "email",
            "sample@example.com",
            "OwnerId",
            "所有者ID",
            "lookup(User)",
            "0053h000004ABCXYZ",
            "CaseNumber",
            "採番",
            "autonumber",
            "00001"
    );

    public static void main(String[] args) {
        // 実行環境のデフォルト文字コードに関わらず日本語を化けさせないため、標準出力をUTF-8に固定する
        System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));

        System.out.println("=== 貼り付けテキスト(サンプル) ===");
        System.out.println(SAMPLE_PASTED_TEXT);

        List<FieldDefinition> fields = ShowAllParser.parse(SAMPLE_PASTED_TEXT);

        System.out.println();
        System.out.println("=== 解析結果: " + fields.size() + "件 ===");
        for (FieldDefinition field : fields) {
            System.out.println(field);
        }

        String emailDomain = "dummy-trial-org.com";
        LocalDate dateRangeStart = LocalDate.of(2020, 1, 1);
        LocalDate dateRangeEnd = LocalDate.of(2026, 7, 3);
        double numberRangeMin = 0;
        double numberRangeMax = 100000;

        DummyDataGenerator generator = new DummyDataGenerator(
                emailDomain, dateRangeStart, dateRangeEnd, numberRangeMin, numberRangeMax);

        int generateCount = 5;
        List<String[]> rows = generator.generate(fields, generateCount);

        System.out.println();
        System.out.println("=== ダミーデータ生成結果(" + generateCount + "件) CSV形式 ===");
        String csv = CsvWriter.toCsv(fields, rows);
        System.out.print(csv);
    }
}
