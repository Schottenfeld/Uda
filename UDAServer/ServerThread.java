package UDAServer;

import java.rmi.*;
import java.rmi.server.UnicastRemoteObject;
import java.sql.*;
import java.util.Vector;

// Implementation of the per thread interface.
public class ServerThread extends UnicastRemoteObject implements ThreadInterface, Runnable
{
	private String DBCONN = "jdbc:odbc:UDAdb";  // Database connection
	private String HOST = "localhost";  // for registring RMI-object

	// Subject number of the independent modules; DB does not allow a nice one like 0 (autovalue)
	public int SUBJECT_IND=7;
	// Communication with DB
	private Connection conDB = null;
	private Statement stmt=null;

	private int ThreadNumber=0; // number of this thread
	private String[] Student = new String[3];

	public ServerThread(int n) throws RemoteException
	{
		super();
		ThreadNumber = n;
		System.setSecurityManager(new RMISecurityManager());
		try
		{
			Naming.rebind("//" + HOST + "/ServerThread"+ThreadNumber, this);
			System.out.println("ServerThread" + ThreadNumber + " bound in registry");
		}
		catch (Exception e)
		{
			System.out.println("Server Thread Exception: " + e.getMessage());
		}
	}

	// Open DB connection, read student data.
	public int Authenticate(String id, String pwd) throws RemoteException
	{
		for (int i=0; i<id.length(); i++)
			if (id.charAt(i)<'0' || id.charAt(i)>'9') return -1; // not numeric, can´t work with query
		try {
			Driver d = (Driver)Class.forName("sun.jdbc.odbc.JdbcOdbcDriver").newInstance();
		} catch (Exception e) {
			System.out.println(e);
		}

		// GET CONNECTION
		try {
			conDB = DriverManager.getConnection(DBCONN,"","");
		} catch(Exception e){
			System.out.println(e);
			return -99;
		}

		// GET CONNECTION WARNINGS
		SQLWarning warning = null;
		try {
			warning = conDB.getWarnings();
			if (warning == null){
				System.out.println("No Warnings");
			}
			while (warning != null) {
				System.out.println("Warning: "+warning);
				warning = warning.getNextWarning();
			}
		} catch (Exception e){
			System.out.println(e);
			return -99;
		}

		// CREATE STATEMENT
		try {
			stmt = conDB.createStatement();
		} catch (Exception e){
			System.out.println(e);
			return -99;
		}

		// EXECUTE QUERY
		ResultSet results=null;
		String qstr;
		try {
			if (strcmp(pwd,"-1"))
				qstr = "select s_name, s_initial, s_familyname from Student " +
						"where s_studentnumber=" + id;
			else
				qstr = "select s_name, s_initial, s_familyname from Student " +
						"where s_studentnumber=" + id + " AND s_password='" + pwd + "'";

			results = stmt.executeQuery(qstr);

			if (!results.next()) return -1;  // no match, User does not exist or wrong pwd
			Student[0] = results.getString(1);
			Student[1] = results.getString(2);
			Student[2] = results.getString(3);
			results.close();
		} catch (Exception e){
			System.out.println(e);
			return -99;
		}
		return 1;
	}

	// Return the student data.
	public String[] getStudent() throws RemoteException
	{
		return Student;
	}

	// Save data to database.
	public String Commit(UDAChosenSM chosenSM) throws java.rmi.RemoteException
	{
		String qstr, strrv; int rv;

		try {
			strrv = StudentStage(chosenSM);
			if (strrv != "") return strrv;

			System.out.println("Write chosen modules."); // 1st the modules
			for (int i=1; i<=chosenSM.ModuleCount; i++)
			{
				qstr = "INSERT INTO mod_chosen values( " +
						chosenSM.Student + ", " + chosenSM.Stage + ", '" +
						chosenSM.ModuleID[i] + "', " + chosenSM.ModuleSubject[i] + ");";
				rv = stmt.executeUpdate(qstr);
				if (rv != 1) return "Can´t write modules to database. (mod_chosen::insert)";
			}
			System.out.println("Write chosen subjects."); // 2nd the subjects
			for (int i=1; i<=chosenSM.SubjectCount; i++)
			{
				qstr = "INSERT INTO sub_chosen values( " +
						chosenSM.Student + ", " + chosenSM.Stage + ", '" +
						chosenSM.SubjectID[i] + "', null);";
				rv = stmt.executeUpdate(qstr);
				if (rv != 1) return "Can´t write subjects to database. (sub_chosen::insert)";
			}

			return "Your chosen subjects and modules were stored successfully.";
		} catch (Exception e){
			System.out.println(e);
			return "Can´t write to database. If the problem continues, visit the office.";
		}
	}

	// Handling for table stud_stage; if no enty exists, it has to be created;
	// if an entry exists, the student has chosen already, and the modules and stage entries
	// have to be deleted.
	private String StudentStage(UDAChosenSM chosenSM)
	{
		ResultSet results=null;
		String qstr; int rv;
		try {
			qstr = "select sst_student, sst_stage "  +
					"FROM stud_stage " +
					"where sst_Stage=" + chosenSM.Stage + " AND sst_student=" + chosenSM.Student;
			results = stmt.executeQuery(qstr);
			if (results.next())
			{
				System.out.println("Student exists. Delete already chosen modules & subjects.");
				qstr = "delete from mod_chosen " +
						"where modc_student=" + chosenSM.Student + " AND modc_stage=" + chosenSM.Stage;
				rv = stmt.executeUpdate(qstr);
				if (rv < 0) return "Can´t update database (mod_chosen::delete).";

				qstr = "delete from sub_chosen " +
						"where subc_student=" + chosenSM.Student + " AND subc_stage=" + chosenSM.Stage;
				rv = stmt.executeUpdate(qstr);
				if (rv < 0) return "Can´t update database (sub_chosen::delete).";

				return "";
			}
			else
			{
				System.out.println("Student does not exist in that stage. Create it.");
				qstr = "INSERT INTO stud_stage values( " +
						chosenSM.Student + ", " + chosenSM.Stage + ", 'combined subject program', 2001 );";
				rv = stmt.executeUpdate(qstr);
				if (rv != 1) return "Can´t write to database (stud_stage::insert).";
			}
			return "";

		} catch (Exception e){
			System.out.println(e);
			return "Can´t write to database (Exception in StudentStage).";
		}
	}

	// Read the chosen subjects and modules from the database.
	public UDAChosenSM getChosenSM(String Student, int Stage) throws java.rmi.RemoteException
	{
		String qstr;
		ResultSet results=null;
		UDAChosenSM chosenSM = new UDAChosenSM();

		try {
			// First subjects and corresponding modules.
			qstr = "select modc_module, modc_subject, mod_name, sub_name "  +
					"FROM mod_chosen, module, subject " +
					"where modc_Stage=" + Stage + " AND modc_student=" + Student + " AND modc_subject<>" + SUBJECT_IND + " AND " +
					"modc_stage=mod_stage AND modc_module=mod_nr AND modc_subject=mod_subject AND mod_subject=sub_nr";
			results = stmt.executeQuery(qstr);

			int i=0, j=0;
			while (results.next())
			{
				i++;
				chosenSM.ModuleID[i] = results.getString(1);
				chosenSM.ModuleName[i] = results.getString(3);
				chosenSM.ModuleSubject[i] = results.getInt(2);
				if (i%2==1)
				{
					j=(int) ((float)i+1)/2;
					chosenSM.SubjectID[j] = chosenSM.ModuleSubject[i];
					chosenSM.SubjectName[j] = results.getString(4);
					chosenSM.SubjectType[j] = "";
				}
			}
			chosenSM.ModuleCount = i;
			chosenSM.SubjectCount = j;
			results.close();

			// Independent modules (subject = SUBJECT_IND):
			qstr = "select modc_module, modc_subject, mod_name "  +
					"FROM mod_chosen, module " +
					"where modc_Stage=" + Stage + " AND modc_student=" + Student + " AND modc_subject=" + SUBJECT_IND + " AND " +
					"modc_stage=mod_stage AND modc_module=mod_nr AND modc_subject=mod_subject";
			results = stmt.executeQuery(qstr);

			i=chosenSM.ModuleCount;
			while (results.next())
			{
				i++;
				chosenSM.ModuleID[i] = results.getString(1);
				chosenSM.ModuleName[i] = results.getString(3);
				chosenSM.ModuleSubject[i] = results.getInt(2);
			}
			chosenSM.ModuleCount = i;
			chosenSM.SubjectCount = j;
			results.close();

			return chosenSM;
		} catch (Exception e){
			System.out.println(e);
			return null;
		}
	}

	// Read the possible subjects for the requested stage.
	public Vector getSubject(int stage) throws java.rmi.RemoteException
	{
		if (conDB==null) return null;
		Vector subVector = new Vector(10,5);

		ResultSet results=null;
		String qstr;
		try {
			qstr = "select Sub_nr, Sub_name, Subp_Notes, sub_timezone "  +
					"FROM Subject INNER JOIN sub_possible ON Subject.Sub_nr = sub_possible.subp_Subjecte " +
					"where (subp_Stage=" + stage + ") AND (subp_Pathway='combined subject program') " +
					"order by Sub_name";
			results = stmt.executeQuery(qstr);

			ResultSetMetaData rsmd = results.getMetaData();
			int numCols = rsmd.getColumnCount();
			int i=0, rowcount = 0;

			while (results.next())
			{
				// create for each row an UDASubject object and put it to the vector
				UDASubject sub = new UDASubject(results.getInt(1), results.getString(2), results.getString(3), results.getString(4));
				subVector.add(sub);
				i++;
			}
			results.close();
			return subVector;
		} catch (Exception e){
			System.out.println(e);
			return null;
		}
	}

	// Read the possible modules for the requested subject and stage.
	public Vector getModule(int subject, int stage) throws java.rmi.RemoteException
	{
		Vector modVector = new Vector(10,5);
		ResultSet results=null;
		String qstr;
		try {
			if (subject == -1) subject = SUBJECT_IND; // if all (independent) modules are requested
			qstr = "select mod_name, mod_weight, mod_nr, mod_timeslot, mod_essential "  +
					"from Module " +
					"where (mod_Stage=" + stage + ") AND (mod_subject=" + subject + ") " +
					"order by mod_name";
			results = stmt.executeQuery(qstr);

			ResultSetMetaData rsmd = results.getMetaData();
			int numCols = rsmd.getColumnCount();
			int i=0, rowcount = 0;

			while (results.next())
			{
				// create for each row an UDAModule object and put it to the vector
				UDAModule mod = new UDAModule(results.getString(1), results.getString(2),
						results.getString(3), results.getString(4), results.getString(5));
				modVector.add(mod);
				i++;
			}
			results.close();
			return modVector;
		} catch (Exception e){
			System.out.println(e);
			return null;
		}
	}
	// Don´t ask ME why comparison of string (==) not always works; but it works this way.
	private boolean strcmp(String str1, String str2)
	{
		if (str1==null && str2==null) return true;
		if (str1==null || str2==null) return false;
		if (str1.length()!=str2.length()) return false;
		for (int i=0; i<str1.length(); i++)
		{
			if (str1.charAt(i)!=str2.charAt(i)) return false;
		}
		return true;
	}

	public void run()
	{
		// The methods of the thread-object do all the work.
	}
}
