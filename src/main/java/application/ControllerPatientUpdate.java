package application;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import view.*;

import java.sql.*;
import java.sql.Connection;

/*
 * Controller class for patient interactions.
 *   register as a new patient.
 *   update patient profile.
 */
@Controller
public class ControllerPatientUpdate {

	@Autowired
	private JdbcTemplate jdbcTemplate;

	/*
	 *  Display patient profile for patient id.
	 */
	@GetMapping("/patient/edit/{id}")
	public String getUpdateForm(@PathVariable int id, Model model) {

		System.out.println(("getUpdateForm " +id));
		// TODO search for patient by id
		PatientView pv = new PatientView();
		pv.setId(id);

		try (Connection con = getConnection();){
			PreparedStatement ps = con.prepareStatement("select last_name, first_name, birth_date, street, city, state, zipcode, doctor_ID from patient where ID=?");
			ps.setInt(1,  id);
			ResultSet rs = ps.executeQuery();

			if (rs.next()) {
				pv.setLast_name(rs.getString(1));
				pv.setFirst_name(rs.getString(2));
				pv.setBirthdate(rs.getString(3));
				pv.setStreet(rs.getString(4));
				pv.setCity(rs.getString(5));
				pv.setState(rs.getString(6));
				pv.setZipcode(rs.getString(7));

				//get doctor's name using doctor_id.
				//set page to display doctor's last name instead of id
				String docName;
				int doctorId = rs.getInt("doctor_ID");
				PreparedStatement docPS = con.prepareStatement("select last_name from doctor where ID=?");
				docPS.setInt(1,  doctorId);
				ResultSet docRS = docPS.executeQuery();
				if(docRS.next()) {
					docName = docRS.getString("last_name");
				}
				else{
					docName = "";
				}
				pv.setPrimaryName(docName);
				model.addAttribute("patient", pv);

				// return editable form with patient data
				return "patient_edit";
			} else {
				model.addAttribute("message", "Doctor not found.");
				model.addAttribute("patient", pv);
				return "patient_get";
			}
			//  if not found, return to home page using return "index";
		}catch (SQLException e){
			model.addAttribute("message", "SQL Error. "+e.getMessage());
			model.addAttribute("patient", pv);
			return "patient_get";
		}

	}

	/*
	 * Process changes from patient_edit form
	 *  Primary doctor, street, city, state, zip can be changed
	 *  ssn, patient id, name, birthdate, ssn are read only in template.
	 */
	@PostMapping("/patient/edit")
	public String updatePatient(PatientView p, Model model) {
		System.out.println("updatePatient " + p);
		try (Connection con = getConnection();){
			// TODO validate doctor last name
			int doctorId;
			PreparedStatement docPS = con.prepareStatement("select ID from doctor where last_name=?");
			docPS.setString(1, p.getPrimaryName());
			ResultSet docRS = docPS.executeQuery();

			//the doctor's id will be based off of the first row returned by query
			// if there is more than 1 doctor with the same last name
			if(docRS.next()) {
				doctorId = docRS.getInt("ID");
			}
			else{
				model.addAttribute("message", "Doctor not found.");
				model.addAttribute("patient", p);
				return "patient_edit";
			}

			//TODO update patient data
			PreparedStatement ps = con.prepareStatement("update patient set street=?, city=?, state=?, zipcode=?, doctor_ID=? where ID=?");
			ps.setString(1, p.getStreet());
			ps.setString(2, p.getCity());
			ps.setString(3, p.getState());
			ps.setString(4, p.getZipcode());
			ps.setInt(5, doctorId);
			ps.setInt(6, p.getId());

			int rc = ps.executeUpdate();

			if(rc == 1){
				model.addAttribute("message", "Update successful");
				model.addAttribute("patient", p);
				return "patient_show";
			}
			else{
				model.addAttribute("message", "Update failed");
				model.addAttribute("patient", p);
				return "patient_edit";
			}

		}catch (SQLException e){
			model.addAttribute("message", "SQL Error."+e.getMessage());
			model.addAttribute("patient", p);
			return "patient_show";
		}

	}

	private Connection getConnection() throws SQLException {
		Connection conn = jdbcTemplate.getDataSource().getConnection();
		return conn;
	}
}