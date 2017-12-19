package cn.ac.nya.nsasm;

import cn.ac.nya.nsasm.NSASM.*;

import java.io.*;
import java.util.*;

/**
 * Created by drzzm on 2017.4.21.
 */
public class Util {

    public interface IPrint {
        void print(Object value);
    }

    public interface IScan {
        String scan();
    }

    public static IPrint print;
    public static IScan scan;

    public static void print(Object value) {
        if (print == null) return;
        print.print(value);
    }

    public static String scan() {
        if (scan == null) return "";
        return scan.scan();
    }

    private static String cleanSymbol(String var, String symbol, String trash) {
        String tmp = var;
        while (tmp.contains(symbol + trash))
            tmp = tmp.replace(symbol + trash, symbol);
        while (tmp.contains(trash + symbol))
            tmp = tmp.replace(trash + symbol, symbol);
        return tmp;
    }

    private static String cleanSymbol(String var, String symbol, String trashA, String trashB) {
        String tmp = var;
        while (tmp.contains(symbol + trashA) || tmp.contains(symbol + trashB))
            tmp = tmp.replace(symbol + trashA, symbol).replace(symbol + trashB, symbol);
        while (tmp.contains(trashA + symbol) || tmp.contains(trashB + symbol))
            tmp = tmp.replace(trashA + symbol, symbol).replace(trashB + symbol, symbol);
        return tmp;
    }

    public static String formatLine(String var) {
        if (var.isEmpty()) return "";
        while (var.contains("\r")) {
            var = var.replace("\r", "");
            if (var.isEmpty()) return "";
        }
        while (var.charAt(0) == '\t' || var.charAt(0) == ' ') {
            var = var.substring(1);
            if (var.isEmpty()) return "";
        }

        String left, right;
        if (var.contains("\'")) {
            left = var.split("\'")[0];
            right = var.substring(left.length());
        } else if (var.contains("\"")) {
            left = var.split("\"")[0];
            right = var.substring(left.length());
            if (right.substring(1).split("\"").length > 1) {
                if (right.substring(1).split("\"")[1].contains("*")) {
                    right = cleanSymbol(right, "*", "\t", " ");
                }
            }
        } else {
            left = var;
            right = "";
        }
        while (left.contains("\t"))
            left = left.replace("\t", " ");
        while (left.contains("  "))
            left = left.replace("  ", " ");
        left = cleanSymbol(left, ",", " ");
        left = cleanSymbol(left, "=", " ");
        left = cleanSymbol(left, "{", "\t", " ");
        left = cleanSymbol(left, "}", "\t", " ");
        left = cleanSymbol(left, "(", "\t", " ");
        left = cleanSymbol(left, ")", "\t", " ");

        return left + right;
    }

    public static String formatCode(String var) {
        String varBuf = ""; Scanner scanner = new Scanner(var);
        while (scanner.hasNextLine()) {
            varBuf = varBuf.concat(formatLine(scanner.nextLine()) + "\n");
        }
        while (varBuf.contains("\n\n")) {
            varBuf = varBuf.replace("\n\n", "\n");
        }
        scanner.close();
        return varBuf;
    }

    public static String repairBrackets(String var, String left, String right) {
        while (var.contains('\n' + left))
            var = var.replace('\n' + left, left);
        var = var.replace(left, left + '\n');
        var = var.replace(right, '\n' + right);
        while (var.contains("\n\n"))
            var = var.replace("\n\n", "\n");
        while (var.contains(left + " "))
            var = var.replace(left + " ", left);
        while (var.contains(" \n" + right))
            var = var.replace(" \n" + right, "\n" + right);
        return var;
    }

    public static String encodeLambda(String var) {
        return var.replace("\n", "\f");
    }

    public static String decodeLambda(String var) {
        return var.replace("\f", "\n");
    }

    public static String formatString(String var) {
        return var.replace("\\\"", "\"").replace("\\\'", "\'")
                .replace("\\\\", "\\").replace("\\n", "\n")
                .replace("\\t", "\t");
    }

    public static String formatLambda(String var) {
        final int IDLE = 0, RUN = 1, DONE = 2;
        int state = IDLE, count = 0, begin = 0, end = 0;

        for (int i = 0; i < var.length(); i++) {
            switch (state) {
                case IDLE:
                    count = begin = end = 0;
                    if (var.charAt(i) == '(') {
                        begin = i;
                        count += 1;
                        state = RUN;
                    }
                    break;
                case RUN:
                    if (var.charAt(i) == '(')
                        count += 1;
                    else if (var.charAt(i) == ')')
                        count -= 1;
                    if (count == 0) {
                        end = i;
                        state = DONE;
                    }
                    break;
                case DONE:
                    String a, b, c;
                    a = var.substring(0, begin);
                    b = var.substring(begin, end + 1);
                    c = var.substring(end + 1);
                    b = encodeLambda(b);
                    var = a + b + c;
                    state = IDLE;
                    break;
                default:
                    return var;
            }
        }

        return var;
    }

    public static String[][] getSegments(String var) {
        LinkedHashMap<String, String> segBuf = new LinkedHashMap<>();
        LinkedList<String> pub = new LinkedList<>();
        String varBuf = var;

        varBuf = formatCode(varBuf);
        varBuf = repairBrackets(varBuf, "{", "}");
        varBuf = repairBrackets(varBuf, "(", ")");
        varBuf = formatCode(varBuf);

        varBuf = formatLambda(varBuf);

        Scanner scanner = new Scanner(varBuf);

        String head, body = "", tmp;
        while (scanner.hasNextLine()) {
            head = scanner.nextLine();
            if (!head.contains("{")) {
                pub.add(head);
                continue;
            }
            head = head.replace("{", "");

            if (scanner.hasNextLine()) {
                tmp = scanner.nextLine();
                while (!tmp.contains("}") && scanner.hasNextLine()) {
                    body = body.concat(tmp + "\n");
                    tmp = scanner.nextLine();
                }
            }

            segBuf.put(head, body);
            body = "";
        }

        String[][] out = new String[segBuf.size() + 1][2];

        out[0][0] = "_pub_" + Integer.toHexString(Integer.signum(var.hashCode()) * var.hashCode());
        out[0][1] = "";
        for (String i : pub) {
            out[0][1] = out[0][1].concat(i + "\n");
        }

        for (int i = 0; i < segBuf.keySet().size(); i++) {
            out[i + 1][0] = (String) segBuf.keySet().toArray()[i];
            out[i + 1][1] = segBuf.get(out[i + 1][0]);
        }

        return out;
    }

    public static String getSegment(String var, String head) {
        String[][] segments = getSegments(var);
        String result = "";
        for (String[] i : segments) {
            if (i[0].equals(head)) {
                if (result.isEmpty())
                    result = i[1];
                else
                    return null;
            }

        }
        return result;
    }

    public static String read(String path) {
        BufferedReader reader;
        try {
            reader = new BufferedReader(new FileReader(path));
        } catch (Exception e) {
            print("File open failed.\n");
            print("At file: " + path + "\n\n");
            return null;
        }

        String str = "";
        try {
            while (reader.ready())
                str = str.concat(reader.readLine() + "\n");
            reader.close();
        } catch (Exception e) {
            print("File read error.\n");
            print("At file: " + path + "\n\n");
            return null;
        }
        return str;
    }

    public static void run(String path) {
        String str = read(path);
        if (str == null) return;

        int heap = 64, stack = 32, regs = 16;

        String conf = getSegment(str, ".<conf>");
        if (conf == null) {
            print("Conf load error.\n");
            print("At file: " + path + "\n\n");
            return;
        }
        if (!conf.isEmpty()) {
            Scanner confReader = new Scanner(conf);
            try {
                String buf;
                while (confReader.hasNextLine()) {
                    buf = confReader.nextLine();
                    switch (buf.split(" ")[0]) {
                        case "heap":
                            heap = Integer.valueOf(buf.split(" ")[1]);
                            break;
                        case "stack":
                            stack = Integer.valueOf(buf.split(" ")[1]);
                            break;
                        case "reg":
                            regs = Integer.valueOf(buf.split(" ")[1]);
                            break;
                    }
                }
            } catch (Exception e) {
                print("Conf load error.\n");
                print("At file: " + path + "\n\n");
                return;
            }
        }

        String[][] code = getSegments(str);
        NSASM nsasm = new NSASM(heap, stack, regs, code);
        nsasm.run();
        print("\nNSASM running finished.\n\n");
    }

    public static void execute(String str) {
        String path = "local";
        if (str == null) return;

        int heap = 64, stack = 32, regs = 16;

        String conf = getSegment(str, ".<conf>");
        if (conf == null) {
            print("Conf load error.\n");
            print("At file: " + path + "\n\n");
            return;
        }
        if (!conf.isEmpty()) {
            Scanner confReader = new Scanner(conf);
            try {
                String buf;
                while (confReader.hasNextLine()) {
                    buf = confReader.nextLine();
                    switch (buf.split(" ")[0]) {
                        case "heap":
                            heap = Integer.valueOf(buf.split(" ")[1]);
                            break;
                        case "stack":
                            stack = Integer.valueOf(buf.split(" ")[1]);
                            break;
                        case "reg":
                            regs = Integer.valueOf(buf.split(" ")[1]);
                            break;
                    }
                }
            } catch (Exception e) {
                print("Conf load error.\n");
                print("At file: " + path + "\n\n");
                return;
            }
        }

        String[][] code = getSegments(str);
        NSASM nsasm = new NSASM(heap, stack, regs, code);
        nsasm.run();
        print("\nNSASM running finished.\n\n");
    }

    public static void console() {
        Util.print("Now in console mode.\n\n");
        String buf;
        int lines = 1; Result result;

        String[][] code = getSegments("nop\n"); //ld func allowed
        NSASM nsasm = new NSASM(64, 32, 16, code);

        while (true) {
            Util.print(lines + " >>> ");
            buf = scan();
            if (buf.length() == 0) {
                lines += 1;
                continue;
            }
            buf = formatLine(buf);

            if (buf.contains("#")) {
                Util.print("<" + buf + ">\n");
                continue;
            }

            result = nsasm.execute(buf);
            if (result == Result.ERR) {
                Util.print("\nNSASM running error!\n");
                Util.print("At line " + lines + ": " + buf + "\n\n");
            } else if (result == Result.ETC) {
                break;
            }
            if (buf.startsWith("run") || buf.startsWith("call")) {
                nsasm.run();
            }

            lines += 1;
        }
    }

    public static void gui() {

    }

}