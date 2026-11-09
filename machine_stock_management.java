import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;

public class MedicineStockManagement {

    static class Medicine {
        String name, batch, expiry;
        int quantity;
        double price;

        Medicine(String name, String batch, int quantity,
                 double price, String expiry) {
            this.name = name;
            this.batch = batch;
            this.quantity = quantity;
            this.price = price;
            this.expiry = expiry;
        }

        boolean isExpired() {
            return !LocalDate.parse(expiry).isAfter(LocalDate.now());
        }
    }

    static ArrayList<Medicine> medicines = new ArrayList<>();

    static String get(Map<String,String> data, String key) {
        return data.getOrDefault(key, "");
    }

    static Map<String,String> readForm(HttpExchange ex)
            throws IOException {
        String body = new String(
            ex.getRequestBody().readAllBytes(),
            StandardCharsets.UTF_8
        );

        Map<String,String> data = new HashMap<>();

        for (String part : body.split("&")) {
            String[] p = part.split("=", 2);
            if (p.length == 2) {
                data.put(
                    URLDecoder.decode(p[0], "UTF-8"),
                    URLDecoder.decode(p[1], "UTF-8")
                );
            }
        }
        return data;
    }

    static void respond(HttpExchange ex, String html)
            throws IOException {
        byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set(
            "Content-Type", "text/html; charset=UTF-8"
        );
        ex.sendResponseHeaders(200, bytes.length);
        ex.getResponseBody().write(bytes);
        ex.close();
    }

    static void redirect(HttpExchange ex) throws IOException {
        ex.getResponseHeaders().set("Location", "/");
        ex.sendResponseHeaders(303, -1);
        ex.close();
    }

    static String page(String message) {
        StringBuilder rows = new StringBuilder();
        int total = 0;
        int expired = 0;

        for (int i = 0; i < medicines.size(); i++) {
            Medicine m = medicines.get(i);
            boolean old = m.isExpired();

            total += m.quantity;
            if (old) expired++;

            rows.append("<tr>")
                .append("<td>").append(m.name).append("</td>")
                .append("<td>").append(m.batch).append("</td>")
                .append("<td>").append(m.quantity).append("</td>")
                .append("<td>₹").append(m.price).append("</td>")
                .append("<td>").append(m.expiry).append("</td>")
                .append("<td class='")
                .append(old ? "expired" : "active")
                .append("'>")
                .append(old ? "EXPIRED" : "AVAILABLE")
                .append("</td><td>");

            rows.append("<form action='/issue' method='post'>")
                .append("<input type='hidden' name='index' value='")
                .append(i).append("'>")
                .append("<input type='number' name='qty' min='1' required placeholder='Qty'>")
                .append("<button>Issue</button></form>");

            rows.append("</td></tr>");
        }

        return """
        <!DOCTYPE html>
        <html>
        <head>
        <title>Medicine Stock Management</title>
        <style>
        * {box-sizing:border-box}
        body {
            margin:0;
            font-family:Arial;
            background:#f1f5f9;
            color:#172554;
        }
        header {
            background:#075985;
            color:white;
            padding:25px;
            text-align:center;
        }
        .container {padding:25px}
        .cards {
            display:flex;
            gap:20px;
            flex-wrap:wrap;
        }
        .card {
            background:white;
            padding:20px;
            border-radius:10px;
            flex:1;
            min-width:160px;
            box-shadow:0 2px 8px #ccc;
        }
        .card h2 {color:#0284c7}
        .section {
            background:white;
            padding:20px;
            margin-top:25px;
            border-radius:10px;
        }
        input {
            padding:11px;
            margin:6px;
            border:1px solid #bbb;
            border-radius:5px;
        }
        button {
            padding:11px 18px;
            background:#0284c7;
            color:white;
            border:0;
            border-radius:5px;
            cursor:pointer;
        }
        button:hover {background:#0369a1}
        table {
            width:100%;
            border-collapse:collapse;
            margin-top:15px;
        }
        th,td {
            padding:12px;
            border:1px solid #ddd;
            text-align:center;
        }
        th {background:#075985;color:white}
        .expired {color:red;font-weight:bold}
        .active {color:green;font-weight:bold}
        .message {color:#0369a1;font-weight:bold}
        </style>
        </head>
        <body>
        <header>
          <h1>💊 Medicine Stock Management System</h1>
          <p>Pharmacy Inventory Dashboard</p>
        </header>

        <div class="container">
        <div class="cards">
          <div class="card">
            <h3>Total Medicine Types</h3>
            <h2>""" + medicines.size() + """
            </h2>
          </div>
          <div class="card">
            <h3>Total Stock</h3>
            <h2>""" + total + """
            </h2>
          </div>
          <div class="card">
            <h3>Expired Batches</h3>
            <h2>""" + expired + """
            </h2>
          </div>
        </div>

        <div class="section">
        <h2>➕ Add New Medicine</h2>
        <form action="/add" method="post">
          <input name="name" placeholder="Medicine Name" required>
          <input name="batch" placeholder="Batch Number" required>
          <input name="quantity" type="number" min="0"
                 placeholder="Quantity" required>
          <input name="price" type="number" min="0" step="0.01"
                 placeholder="Price" required>
          <input name="expiry" type="date" required>
          <button>Add Medicine</button>
        </form>
        </div>

        <div class="section">
        <h2>📦 Stock Management</h2>
        <form action="/stock" method="post">
          <input name="batch" placeholder="Batch Number" required>
          <input name="qty" type="number" min="1"
                 placeholder="Quantity" required>
          <select name="operation">
            <option value="add">Add Stock</option>
            <option value="reduce">Reduce Stock</option>
          </select>
          <button>Update Stock</button>
        </form>
        </div>

        <div class="section">
        <h2>💊 Available Medicines</h2>
        <p class="message">""" + message + """
        </p>
        <table>
        <tr>
          <th>Medicine</th>
          <th>Batch</th>
          <th>Quantity</th>
          <th>Price</th>
          <th>Expiry Date</th>
          <th>Status</th>
          <th>Issue Medicine</th>
        </tr>
        """ + rows + """
        </table>
        </div>
        </div>
        </body>
        </html>
        """;
    }

    static String message = "Welcome to Medicine Stock Management";

    public static void main(String[] args) throws Exception {

        HttpServer server = HttpServer.create(
            new InetSocketAddress(8081), 0
        );

        server.createContext("/", ex -> {
            if (ex.getRequestURI().getPath().equals("/")) {
                respond(ex, page(message));
            } else {
                ex.sendResponseHeaders(404, -1);
                ex.close();
            }
        });

        server.createContext("/add", ex -> {
            Map<String,String> d = readForm(ex);

            try {
                String name = get(d,"name").trim();
                String batch = get(d,"batch").trim();
                int qty = Integer.parseInt(get(d,"quantity"));
                double price = Double.parseDouble(get(d,"price"));
                String expiry = get(d,"expiry");

                LocalDate.parse(expiry);

                boolean exists = medicines.stream()
                    .anyMatch(m -> m.batch.equalsIgnoreCase(batch));

                if (name.isEmpty() || batch.isEmpty() ||
                    qty < 0 || price < 0 || exists) {
                    message = "Invalid details or duplicate batch number!";
                } else {
                    medicines.add(new Medicine(
                        name,batch,qty,price,expiry
                    ));
                    message = "Medicine added successfully!";
                }
            } catch (Exception e) {
                message = "Invalid medicine details!";
            }
            redirect(ex);
        });

        server.createContext("/stock", ex -> {
            Map<String,String> d = readForm(ex);

            String batch = get(d,"batch");
            String operation = get(d,"operation");

            try {
                int qty = Integer.parseInt(get(d,"qty"));

                Medicine found = medicines.stream()
                    .filter(m -> m.batch.equalsIgnoreCase(batch))
                    .findFirst().orElse(null);

                if (found == null || qty <= 0) {
                    message = "Medicine not found or invalid quantity!";
                } else if (operation.equals("reduce")) {
                    if (found.isExpired() ||
                        qty > found.quantity) {
                        message = "Cannot reduce: expired or insufficient stock!";
                    } else {
                        found.quantity -= qty;
                        message = "Stock reduced successfully!";
                    }
                } else {
                    found.quantity += qty;
                    message = "Stock added successfully!";
                }
            } catch (Exception e) {
                message = "Invalid stock details!";
            }
            redirect(ex);
        });

        server.createContext("/issue", ex -> {
            Map<String,String> d = readForm(ex);

            try {
                int index = Integer.parseInt(get(d,"index"));
                int qty = Integer.parseInt(get(d,"qty"));

                if (index < 0 || index >= medicines.size()) {
                    message = "Invalid medicine!";
                } else {
                    Medicine m = medicines.get(index);

                    if (m.isExpired()) {
                        message = "ERROR: Expired medicine cannot be issued!";
                    } else if (qty <= 0 || qty > m.quantity) {
                        message = "Insufficient stock or invalid quantity!";
                    } else {
                        m.quantity -= qty;
                        message = "Medicine issued successfully!";
                    }
                }
            } catch (Exception e) {
                message = "Invalid issuing details!";
            }
            redirect(ex);
        });

        server.setExecutor(null);
        server.start();

        System.out.println("Medicine Stock Management Started!");
        System.out.println("Open http://localhost:8080");
    }
}