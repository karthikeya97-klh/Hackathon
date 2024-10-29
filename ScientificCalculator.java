import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.concurrent.*;

public class ScientificCalculator extends JFrame implements ActionListener {
    private JTextField textField;
    private ExecutorService executorService; 
    private JButton calculateButton; 
    private JLabel statusLabel; 

    public ScientificCalculator() {
        setTitle("Scientific Calculator with Multithreading");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        textField = new JTextField();
        textField.setEditable(false);
        textField.setPreferredSize(new Dimension(300, 40));
        add(textField, BorderLayout.NORTH);
        statusLabel = new JLabel(" ");
        add(statusLabel, BorderLayout.SOUTH);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new GridLayout(9, 4));
        buttonPanel.setBackground(Color.ORANGE);
        buttonPanel.setForeground(Color.PINK);

        String[] buttonLabels = {
                "1", "2", "3", "/", "4", "5", "6", "*", "7", "8", "9", "-", "0", ".", "=", "+", "Clear",
                "(", ")", "^", "sqrt", "cbrt", "log", "sin", "cos", "tan", "asin", "acos", "atan", "!", "%", "|x|"
        };

        for (String label : buttonLabels) {
            JButton button = new JButton(label);
            button.addActionListener(this);
            buttonPanel.add(button);
        }
        calculateButton = new JButton("Calculate");
        calculateButton.addActionListener(this);
        calculateButton.setEnabled(false);
        buttonPanel.add(calculateButton);

        add(buttonPanel, BorderLayout.CENTER);
        executorService = Executors.newFixedThreadPool(10);

        pack();
        setVisible(true);
    }

    public void actionPerformed(ActionEvent event) {
        String command = event.getActionCommand();
        String expression = textField.getText();

        switch (command) {
            case "=":
                disableButtons(true);
                statusLabel.setText("Calculating...");
                executorService.submit(() -> {
                    try {
                        double result = evaluateExpression(expression);
                        SwingUtilities.invokeLater(() -> {
                            textField.setText(Double.toString(result));
                            statusLabel.setText(" ");
                        });
                    } catch (ArithmeticException e) {
                        SwingUtilities.invokeLater(() -> {
                            textField.setText("Error: " + e.getMessage());
                            statusLabel.setText(" ");
                        });
                    } finally {
                        disableButtons(false);
                    }
                });
                break;
            case "Clear":
                textField.setText("");
                break;
            case "<=":
                if (!expression.isEmpty()) {
                    String newExpression = expression.substring(0, expression.length() - 1);
                    textField.setText(newExpression);
                }
                break;
            default:
                textField.setText(expression + command);
                break;
        }
    }
    private void disableButtons(boolean disable) {
        for (Component comp : getContentPane().getComponents()) {
            if (comp instanceof JPanel) {
                for (Component button : ((JPanel) comp).getComponents()) {
                    if (button instanceof JButton) {
                        button.setEnabled(!disable);
                    }
                }
            }
        }
    }

    private double evaluateExpression(String expression) {
        return new ExpressionParser().parse(expression);
    }

    private static class ExpressionParser {
        private int pos = -1;
        private int ch;

        private void nextChar(String expression) {
            ch = (++pos < expression.length()) ? expression.charAt(pos) : -1;
        }

        private boolean eat(int charToEat, String expression) {
            while (ch == ' ') nextChar(expression);
            if (ch == charToEat) {
                nextChar(expression);
                return true;
            }
            return false;
        }

        private double parse(String expression) {
            nextChar(expression);
            double x = parseExpression(expression);
            if (pos < expression.length()) throw new RuntimeException("Unexpected: " + (char) ch);
            return x;
        }

        private double parseExpression(String expression) {
            double x = parseTerm(expression);
            while (true) {
                if (eat('+', expression)) x += parseTerm(expression);
                else if (eat('-', expression)) x -= parseTerm(expression);
                else return x;
            }
        }

        private double parseTerm(String expression) {
            double x = parseFactor(expression);
            while (true) {
                if (eat('*', expression)) x *= parseFactor(expression);
                else if (eat('/', expression)) x /= parseFactor(expression);
                else if (eat('^', expression)) x = Math.pow(x, parseFactor(expression));
                else return x;
            }
        }

        private double parseFactor(String expression) {
            if (eat('+', expression)) return parseFactor(expression);
            if (eat('-', expression)) return -parseFactor(expression);

            double x;
            int startPos = this.pos;
            if (eat('(', expression)) {
                x = parseExpression(expression);
                eat(')', expression);
            } else if ((ch >= '0' && ch <= '9') || ch == '.') {
                while ((ch >= '0' && ch <= '9') || ch == '.') nextChar(expression);
                x = Double.parseDouble(expression.substring(startPos, this.pos));
            } else if (ch >= 'a' && ch <= 'z') {
                while (ch >= 'a' && ch <= 'z') nextChar(expression);
                String func = expression.substring(startPos, this.pos);
                x = parseFactor(expression);
                switch (func) {
                    case "sqrt":
                        x = Math.sqrt(x);
                        break;
                    case "cbrt":
                        x = Math.cbrt(x);
                        break;
                    case "log":
                        x = Math.log10(x);
                        break;
                    case "sin":
                        x = Math.sin(Math.toRadians(x));
                        break;
                    case "cos":
                        x = Math.cos(Math.toRadians(x));
                        break;
                    case "tan":
                        x = Math.tan(Math.toRadians(x));
                        break;
                    case "asin":
                        x = Math.toDegrees(Math.asin(x));
                        break;
                    case "acos":
                        x = Math.toDegrees(Math.acos(x));
                        break;
                    case "atan":
                        x = Math.toDegrees(Math.atan(x));
                        break;
                    case "!":
                        x = factorial((int) x);
                        break;
                    case "%":
                        x = x / 100.0;
                        break;
                    case "|x|":
                        x = Math.abs(x);
                        break;
                    default:
                        throw new RuntimeException("Unknown function: " + func);
                }
            } else {
                throw new RuntimeException("Unexpected: " + (char) ch);
            }

            return x;
        }

        private int factorial(int n) {
            if (n < 0) throw new RuntimeException("Factorial is not defined for negative numbers");
            if (n == 0) return 1;
            int fact = 1;
            for (int i = 1; i <= n; i++) {
                fact *= i;
            }
            return fact;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ScientificCalculator::new);
    }
}
