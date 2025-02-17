package application;

import java.sql.*;
import java.time.LocalDate;
import java.util.Random;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import view.*;

@Controller
public class ControllerPrescriptionCreate {

	@Autowired
	private JdbcTemplate jdbcTemplate;

	/*
	 * Doctor requests blank form for new prescription.
	 */
	@GetMapping("/prescription/new")
	public String getPrescriptionForm(Model model) {
		model.addAttribute("prescription", new PrescriptionView());
		return "prescription_create";
	}

	// process data entered on prescription_create form
	@PostMapping("/prescription")
	public String createPrescription(PrescriptionView p, Model model) {

		System.out.println("createPrescription " + p);

		try (Connection con = getConnection();){
			/*
			 * valid doctor name and id
			 */
			//TODO
			PreparedStatement docPS = con.prepareStatement("select last_name, first_name, ID from doctor where last_name = ? and first_name = ? and id = ?");
			docPS.setString(1, p.getDoctorLastName());
			docPS.setString(2, p.getDoctorFirstName());
			docPS.setInt(3, p.getDoctor_id());
			ResultSet docRS = docPS.executeQuery();
			if (!docRS.next()) {
				model.addAttribute("message", "Error. incorrect doctor records");
				model.addAttribute("prescription", p);
				return "prescription_create";
			}
			/*
			 * valid patient name and id
			 */
			//TODO
			PreparedStatement patPS = con.prepareStatement("select last_name, first_name, ID from patient where last_name = ? and first_name = ? and id = ?");
			patPS.setString(1, p.getPatientLastName());
			patPS.setString(2, p.getPatientFirstName());
			patPS.setInt(3, p.getPatient_id());
			ResultSet patRS = patPS.executeQuery();
			if (!patRS.next()) {
				model.addAttribute("message", "Error. incorrect patient records");
				model.addAttribute("prescription", p);
				return "prescription_create";
			}

			/*
			 * valid drug name
			 */
			//TODO
			int drugID;
			PreparedStatement drugPS = con.prepareStatement("select id from drug where name = ?");
			drugPS.setString(1, p.getDrugName());

			ResultSet drugRS = drugPS.executeQuery();
			if (drugRS.next()){
				drugID = drugRS.getInt("id");
			}
			else {
				model.addAttribute("message", "Error. drug not found");
				model.addAttribute("prescription", p);
				return "prescription_create";
			}

			/*
			 * insert prescription
			 */
			//TODO
			PreparedStatement ps = con.prepareStatement("insert into prescription(doctor_ID, patient_ID, drug_ID, drug_quantity, max_number_refill, date_created) values(?,?,?,?,?,?)",
					Statement.RETURN_GENERATED_KEYS);
			ps.setInt(1, p.getDoctor_id());
			ps.setInt(2, p.getPatient_id());
			ps.setInt(3, drugID);
			ps.setInt(4, p.getQuantity());
			ps.setInt(5, p.getRefills());
			ps.setDate(6, Date.valueOf(LocalDate.now()));

			ps.executeUpdate();
			ResultSet rs = ps.getGeneratedKeys();
			if (rs.next()) p.setRxid(rs.getInt(1));

			model.addAttribute("message", "Prescription created successfully");
			model.addAttribute("prescription", p);
			return "prescription_show";

		} catch (SQLException e){
			model.addAttribute("message", "SQL Error."+e.getMessage());
			model.addAttribute("prescription", p);
			return "prescription_create";
		}

	}

	private Connection getConnection() throws SQLException {
		Connection conn = jdbcTemplate.getDataSource().getConnection();
		return conn;
	}

}
