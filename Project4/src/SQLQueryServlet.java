import javax.servlet.*;
import javax.servlet.http.*;
import java.io.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;

public class SQLQueryServlet extends HttpServlet {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Connection connection;
	private Statement statement;

	
	// Setup database connection.
	@Override
	public void init(ServletConfig config) throws ServletException {
		
		super.init(config);
		
		// Connected to database.
		try {
			
			// Use the xml file for variables. 
			Class.forName(config.getInitParameter("databaseDriver"));
			
			connection = DriverManager.getConnection(config.getInitParameter("databaseName"),
						 config.getInitParameter("username"), config.getInitParameter("password"));
			statement = connection.createStatement();
		}

		catch (Exception e) {
			e.printStackTrace();
			throw new UnavailableException(e.getMessage());
		}
	}

	// Process the get request from the jsp.
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
		
		throws ServletException, IOException {
			String textBox = request.getParameter("textBox");
			String textBoxLowerCase = textBox.toLowerCase();
			String result = null;
		
			// Is it a select statement or not?
			if (textBoxLowerCase.contains("select")) {
	
				try {
					result = doSelectQuery(textBoxLowerCase);
				} 
				
				catch (SQLException e) {
					result = "<span>" + e.getMessage() + "</span>";
	
					e.printStackTrace();
				}
			}
			else { 
				
				// Do insert,update, delete, create, drop.
				try {
					result = doUpdateQuery(textBoxLowerCase);
				}
				
				catch(SQLException e) {
					result = "<span>" + e.getMessage() + "</span>";
					e.printStackTrace();
				}
		}

		HttpSession session = request.getSession();
		session.setAttribute("result", result);
		session.setAttribute("textBox", textBox);
		RequestDispatcher dispatcher = getServletContext().getRequestDispatcher("/index.jsp");
		dispatcher.forward(request, response);
	}

	// Process the post request from the jsp.
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
		throws ServletException, IOException {
			doGet(request, response);
	}

	// Execute a select query and create a table with the results.
	public String doSelectQuery(String textBox) throws SQLException {
		
		String res;
		
		// Run sql command that was provided.
		ResultSet table = statement.executeQuery(textBox);
		
		// Query results.
		ResultSetMetaData metaData = table.getMetaData();
		
		// Table columns html.
		int numOfColumns = metaData.getColumnCount();
		
		// Table to open the html
		String TOpenHTML = "<div class='container-fluid'><div class='row justify-content-center'><div class='table-responsive-sm-10 table-responsive-md-10 table-responsive-lg-10'><table class='table'>";
		
		// Table html columns.
		String TColumnsHTML = "<thead class='thead-dark'><tr>";
		
		for (int i = 1; i <= numOfColumns; i++) {
			TColumnsHTML += "<th scope='col'>" + metaData.getColumnName(i) + "</th>";
		}
		
		// close the html tale column element.
		TColumnsHTML += "</tr></thead>"; 

		// Table html body/rows.
		String TBodyHTML = "<tbody>";
		
		// Get row info.
		while (table.next()) {
			TBodyHTML += "<tr>";
			for (int i = 1; i <= numOfColumns; i++) {
				
				// If first element.
				if (i == 1)
					TBodyHTML += "<td scope'row'>" + table.getString(i) + "</th>";
				else
					TBodyHTML += "<td>" + table.getString(i) + "</th>";
			}
			TBodyHTML += "</tr>";
		}

		TBodyHTML += "</tbody>";

		// Closing html.
		String TCloseHTML = "</table></div></div></div>";
		res = TOpenHTML + TColumnsHTML + TBodyHTML + TCloseHTML;

		return res;
	}
	
	private String doUpdateQuery(String textBoxLowerCase) throws SQLException {
		String res = null;
		int numOfRowsUpdated = 0;
		
		// Get number of shipment with quantity >= 100 before update/insert.
		ResultSet beforeCheck = statement.executeQuery("select COUNT(*) from shipments where quantity >= 100");
		beforeCheck.next();
		int numOfShipmentsGreaterBefore = beforeCheck.getInt(1);
		
		// Create temp table for the case of updating suppliers status's.
		statement.executeUpdate("create table beforeShipments like shipments");
		
		// Copy table over to new temp table.
		statement.executeUpdate("insert into beforeShipments select * from shipments");
		
		// Execute update.
		numOfRowsUpdated = statement.executeUpdate(textBoxLowerCase);
		res = "<div> The statement executed succesfully.</div><div>" + numOfRowsUpdated + " row(s) affected</div>";
		
		// Get number of shipment with quantity >= 100 before update/insert.
		ResultSet afterQuantityCheck = statement.executeQuery("select COUNT(*) from shipments where quantity >= 100");
		afterQuantityCheck.next();
		int numOfShipmentsGreaterAfter = afterQuantityCheck.getInt(1);
		
		res += "<div>" + numOfShipmentsGreaterBefore + " < " + numOfShipmentsGreaterAfter + "</div>";
		
		// Update the status of suppliers if quantity is greater than 100.
		if(numOfShipmentsGreaterBefore < numOfShipmentsGreaterAfter) {
			
			// Increase suppliers status by 5.
			// Handle updates into shipments by using a left join with shipments and temp table.
			int numberOfRowsAffectedAfter5 = statement.executeUpdate("update suppliers set status = status + 5 where snum in ( select distinct snum from shipments left join shipmentsBeforeUpdate using (snum, pnum, jnum, quantity) where shipmentsBeforeUpdate.snum is null)");
			res += "<div>Business Logic Detected! - Updating Supplier Status</div>";
			res += "<div>Business Logic Updated " + numberOfRowsAffectedAfter5 + " Supplier(s) status marks</div>";
		}
		
		statement.executeUpdate("drop table shipmentsBeforeUpdate");
		
		return res;
	}

}
