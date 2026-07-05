import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * 画面部品の配置とイベント制御を担当するクラス(JFrame継承)。
 * tkinterでいう Tk()のメインウィンドウ + ウィジェット配置 + bind(コールバック登録) をまとめて行う場所に相当する。
 */
public class MainWindow extends JFrame {

    private final JTextArea pasteArea = new JTextArea(8, 60);
    private final FieldTableModel tableModel = new FieldTableModel();
    private final JTable previewTable = new JTable(tableModel);

    private final JTextField emailDomainField = new JTextField("example-dummy.com", 16);
    private final JSpinner dateStartSpinner;
    private final JSpinner dateEndSpinner;
    private final JSpinner numberMinSpinner = new JSpinner(new SpinnerNumberModel(0.0, -1_000_000_000.0, 1_000_000_000.0, 1.0));
    private final JSpinner numberMaxSpinner = new JSpinner(new SpinnerNumberModel(100000.0, -1_000_000_000.0, 1_000_000_000.0, 1.0));
    private final JSpinner countSpinner = new JSpinner(new SpinnerNumberModel(10, 1, 100000, 1));
    private final JTextArea resultArea = new JTextArea(10, 60);

    private List<FieldDefinition> parsedFields = new ArrayList<>();
    private String generatedCsv = "";

    public MainWindow() {
        super("Salesforceダミーデータ生成ツール");

        Date today = toDate(LocalDate.now());
        Date twoYearsAgo = toDate(LocalDate.now().minusYears(2));
        dateStartSpinner = new JSpinner(new SpinnerDateModel(twoYearsAgo, null, null, Calendar.DAY_OF_MONTH));
        dateEndSpinner = new JSpinner(new SpinnerDateModel(today, null, null, Calendar.DAY_OF_MONTH));
        dateStartSpinner.setEditor(new JSpinner.DateEditor(dateStartSpinner, "yyyy-MM-dd"));
        dateEndSpinner.setEditor(new JSpinner.DateEditor(dateEndSpinner, "yyyy-MM-dd"));

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(8, 8));

        add(buildPastePanel(), BorderLayout.NORTH);

        JSplitPane splitPane = new JSplitPane(
                JSplitPane.VERTICAL_SPLIT, buildPreviewPanel(), buildGenerationPanel());
        splitPane.setResizeWeight(0.4);
        add(splitPane, BorderLayout.CENTER);

        setSize(900, 700);
        setLocationRelativeTo(null);
    }

    private JPanel buildPastePanel() {
        JPanel panel = new JPanel(new BorderLayout(4, 4));
        panel.setBorder(BorderFactory.createTitledBorder(
                "Show all画面からコピーした項目一覧を貼り付け"));

        pasteArea.setLineWrap(false);
        panel.add(new JScrollPane(pasteArea), BorderLayout.CENTER);

        JButton analyzeButton = new JButton("解析");
        analyzeButton.addActionListener(e -> onAnalyze());
        JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonRow.add(analyzeButton);
        panel.add(buttonRow, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel buildPreviewPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("解析結果プレビュー"));
        previewTable.setFillsViewportHeight(true);
        panel.add(new JScrollPane(previewTable), BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildGenerationPanel() {
        JPanel panel = new JPanel(new BorderLayout(4, 4));
        panel.add(buildSettingsPanel(), BorderLayout.NORTH);

        JPanel resultPanel = new JPanel(new BorderLayout(4, 4));
        resultPanel.setBorder(BorderFactory.createTitledBorder("生成結果プレビュー(CSV)"));
        resultArea.setEditable(false);
        resultArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        resultPanel.add(new JScrollPane(resultArea), BorderLayout.CENTER);

        JButton saveButton = new JButton("CSV保存");
        saveButton.addActionListener(e -> onSaveCsv());
        JPanel saveRow = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        saveRow.add(saveButton);
        resultPanel.add(saveRow, BorderLayout.SOUTH);

        panel.add(resultPanel, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildSettingsPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("生成設定"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.anchor = GridBagConstraints.WEST;

        int row = 0;
        addSettingRow(panel, gbc, row++, "メールドメイン(組織ごとに変更):", emailDomainField);

        JPanel dateRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        dateRow.add(dateStartSpinner);
        dateRow.add(new JLabel("〜"));
        dateRow.add(dateEndSpinner);
        addSettingRow(panel, gbc, row++, "日付範囲(date/datetime用):", dateRow);

        JPanel numberRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        numberRow.add(numberMinSpinner);
        numberRow.add(new JLabel("〜"));
        numberRow.add(numberMaxSpinner);
        addSettingRow(panel, gbc, row++, "数値範囲(number/currency用):", numberRow);

        JPanel countRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        countRow.add(countSpinner);
        JButton generateButton = new JButton("生成");
        generateButton.addActionListener(e -> onGenerate());
        countRow.add(generateButton);
        addSettingRow(panel, gbc, row++, "生成件数:", countRow);

        return panel;
    }

    private void addSettingRow(JPanel panel, GridBagConstraints gbc, int row, String labelText, JComponent field) {
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        panel.add(new JLabel(labelText), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(field, gbc);
    }

    private void onAnalyze() {
        List<FieldDefinition> fields = ShowAllParser.parse(pasteArea.getText());

        if (fields.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "項目を解析できませんでした。貼り付け内容を確認してください。",
                    "解析結果", JOptionPane.WARNING_MESSAGE);
        }

        parsedFields = fields;
        tableModel.setFields(fields);
    }

    private void onGenerate() {
        if (parsedFields.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "先に「解析」を実行してください。",
                    "生成できません", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String emailDomain = emailDomainField.getText().trim();
        if (emailDomain.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "メールドメインを入力してください。",
                    "入力エラー", JOptionPane.WARNING_MESSAGE);
            return;
        }

        LocalDate dateStart = toLocalDate((Date) dateStartSpinner.getValue());
        LocalDate dateEnd = toLocalDate((Date) dateEndSpinner.getValue());
        if (dateStart.isAfter(dateEnd)) {
            JOptionPane.showMessageDialog(this,
                    "日付範囲の開始が終了より後になっています。",
                    "入力エラー", JOptionPane.WARNING_MESSAGE);
            return;
        }

        double numberMin = ((Number) numberMinSpinner.getValue()).doubleValue();
        double numberMax = ((Number) numberMaxSpinner.getValue()).doubleValue();
        if (numberMin > numberMax) {
            JOptionPane.showMessageDialog(this,
                    "数値範囲の最小値が最大値より大きくなっています。",
                    "入力エラー", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int count = (Integer) countSpinner.getValue();

        DummyDataGenerator generator = new DummyDataGenerator(
                emailDomain, dateStart, dateEnd, numberMin, numberMax);
        List<String[]> rows = generator.generate(parsedFields, count);
        generatedCsv = CsvWriter.toCsv(parsedFields, rows);

        resultArea.setText(generatedCsv);
        resultArea.setCaretPosition(0);
    }

    private void onSaveCsv() {
        if (generatedCsv.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "先に「生成」を実行してください。",
                    "保存できません", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File("dummy_data.csv"));
        int selection = chooser.showSaveDialog(this);
        if (selection != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File file = chooser.getSelectedFile();
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
            writer.write(generatedCsv);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this,
                    "保存に失敗しました: " + ex.getMessage(),
                    "エラー", JOptionPane.ERROR_MESSAGE);
            return;
        }

        JOptionPane.showMessageDialog(this,
                "保存しました: " + file.getAbsolutePath(),
                "保存完了", JOptionPane.INFORMATION_MESSAGE);
    }

    private static Date toDate(LocalDate localDate) {
        return Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    private static LocalDate toLocalDate(Date date) {
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    private static class FieldTableModel extends AbstractTableModel {
        private static final String[] COLUMNS = {"API参照名", "表示ラベル", "データ型", "値", "必須"};
        private List<FieldDefinition> fields = new ArrayList<>();

        void setFields(List<FieldDefinition> fields) {
            this.fields = fields;
            fireTableDataChanged();
        }

        @Override
        public int getRowCount() {
            return fields.size();
        }

        @Override
        public int getColumnCount() {
            return COLUMNS.length;
        }

        @Override
        public String getColumnName(int column) {
            return COLUMNS[column];
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            FieldDefinition field = fields.get(rowIndex);
            switch (columnIndex) {
                case 0: return field.getApiName();
                case 1: return field.getLabel();
                case 2: return field.getRawType();
                case 3: return field.getSampleValue();
                case 4: return field.isRequired() ? "○" : "";
                default: return "";
            }
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return false;
        }
    }
}
