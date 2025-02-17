package application;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import view.*;

/*
 * Controller class for patient interactions.
 *   register as a new patient.
 *   update patient profile.
 */
@Controller
public class ControllerPatientCreate {
	
	@Autowired
	private JdbcTemplate jdbcTemplate;
	
	
	/*
	 * Request blank patient registration form.
	 */
	@GetMapping("/patient/new")
	public String getNewPatientForm(Model model) {
		// return blank form for new patient registration
		model.addAttribute("patient", new PatientView());
		return "patient_register";
	}
	
	/*
	 * Process data from the patient_register form
	 */
	@PostMapping("/patient/new")
	public String createPatient(PatientView patient, Model model){

		try (Connection con = getConnection()) {
			// Validate doctor's last name and find doctor_id
			PreparedStatement getDoctor = con.prepareStatement("SELECT id FROM doctor WHERE last_name = ?");
			getDoctor.setString(1, patient.getPrimaryName()); // Assuming primaryName is storing doctor's last name
			ResultSet rs = getDoctor.executeQuery();

			int doctorId;
			if (rs.next()) {
				doctorId = rs.getInt("id");
			} else {
				model.addAttribute("message", "Error: No doctor found with last name '" + patient.getPrimaryName() + "'.");
				model.addAttribute("patient", patient);
				return "patient_register";
			}

			// Insert new patient
			PreparedStatement ps = con.prepareStatement(
					"INSERT INTO patient(last_name, first_name, birth_date, street, city, state, zipcode, doctor_id, ssn) " +
							"VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
			ps.setString(1, patient.getLast_name());
			ps.setString(2, patient.getFirst_name());
			ps.setString(3, patient.getBirthdate());
			ps.setString(4, patient.getStreet());
			ps.setString(5, patient.getCity());
			ps.setString(6, patient.getState());
			ps.setString(7, patient.getZipcode());
			ps.setInt(8, doctorId); // assign validated doctor_id
			ps.setString(9, patient.getSsn());

			ps.executeUpdate();
			rs = ps.getGeneratedKeys();
			if (rs.next()) patient.setId(rs.getInt(1));

			model.addAttribute("message", "Registration successful.");
			model.addAttribute("patient", patient);
			return "patient_show";

		} catch (SQLException e) {
			model.addAttribute("message", "SQL Error: " + e.getMessage());
			model.addAttribute("patient", patient);
			return "patient_register";
		}
	}
	
	/*
	 * Request blank form to search for patient by id and name
	 */
	@GetMapping("/patient/edit")
	public String getSearchForm(Model model) {
		model.addAttribute("patient", new PatientView());
		return "patient_get";
	}
	
	/*
	 * Perform search for patient by patient id and name.
	 */
	@PostMapping("/patient/show")
	public String showPatient(PatientView patient, Model model) {

		System.out.println("showPatient "+ patient);  // debugging in console

		try (Connection con = getConnection()) {

			PreparedStatement ps = con.prepareStatement("select last_name, first_name, birth_date, street, city, state, zipcode, doctor_id from patient where id=? and last_name=?");
			ps.setInt(1, patient.getId());
			ps.setString(2, patient.getLast_name());

			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				patient.setLast_name(rs.getString(1));
				patient.setFirst_name(rs.getString(2));
				patient.setBirthdate(rs.getString(3));
				patient.setStreet(rs.getString(4));
				patient.setCity(rs.getString(5));
				patient.setState(rs.getString(6));
				patient.setZipcode(rs.getString(7));
				patient.setPrimaryName(rs.getString(8));

				model.addAttribute("patient", patient);
				System.out.println("end getPatient "+patient);  // debug
				return "patient_show";

			} else {
				model.addAttribute("message", "Patient not found.");
				model.addAttribute("patient", patient);
				return "patient_get";
			}

		} catch (SQLException e) {
			System.out.println("SQL error in getPatient "+e.getMessage());
			model.addAttribute("message", "SQL Error."+e.getMessage());
			model.addAttribute("patient", patient);
			return "patient_show";
		}
	}
	
	/*
	 * return JDBC Connection using jdbcTemplate in Spring Server
	 */

	private Connection getConnection() throws SQLException {
		Connection conn = jdbcTemplate.getDataSource().getConnection();
		return conn;
	}
}
