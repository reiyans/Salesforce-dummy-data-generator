import javax.swing.SwingUtilities;

/**
 * GUI版のエントリーポイント。tkinterでいう root.mainloop() を起動する場所に相当する。
 * 画面部品の生成・更新は必ずEDT(Event Dispatch Thread)上で行う。
 */
public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MainWindow().setVisible(true));
    }
}
