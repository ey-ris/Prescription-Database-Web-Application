package application;


import java.sql.*;
import java.time.LocalDate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import view.PrescriptionView;

@Controller
public class ControllerPrescriptionFill {

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@GetMapping("/prescription/fill")
	public String getfillForm(Model model) {
		model.addAttribute("prescription", new PrescriptionView()); // Ensure model has prescription
		return "prescription_fill";
	}

	@PostMapping("/prescription/fill")
	public String processFillForm(PrescriptionView p, Model model) {
		model.addAttribute("prescription", p); // Always add prescription to the model

		try (Connection con = getConnection()) {
			System.out.println("Processing Prescription Fill for RXID: " + p.getRxid());

			// 1. Validate Pharmacy
			PreparedStatement pharmacyPS = con.prepareStatement(
					"SELECT ID, phone FROM pharmacy WHERE name = ? AND address = ?"
			);
			pharmacyPS.setString(1, p.getPharmacyName());
			pharmacyPS.setString(2, p.getPharmacyAddress());
			ResultSet pharmacyRS = pharmacyPS.executeQuery();
			if (pharmacyRS.next()) {
				p.setPharmacyID(pharmacyRS.getInt("ID"));
				p.setPharmacyPhone(pharmacyRS.getString("phone"));
			} else {
				model.addAttribute("message", "Error: Pharmacy not found.");
				return "prescription_fill";
			}

			// 2. Validate Prescription
			PreparedStatement prescriptionPS = con.prepareStatement(
					"SELECT drug_ID, drug_quantity, max_number_refill FROM prescription WHERE RXID = ?"
			);
			prescriptionPS.setInt(1, p.getRxid());
			ResultSet prescriptionRS = prescriptionPS.executeQuery();
			if (prescriptionRS.next()) {
				p.setDrugName(String.valueOf(prescriptionRS.getInt("drug_ID")));
				p.setQuantity(prescriptionRS.getInt("drug_quantity"));
				p.setRefills(prescriptionRS.getInt("max_number_refill"));
			} else {
				model.addAttribute("message", "Error: Prescription not found.");
				return "prescription_fill";
			}

			// 3. Retrieve Fill Count
			PreparedStatement fillCountPS = con.prepareStatement(
					"SELECT COUNT(*) AS fill_count FROM prescription_filled WHERE prescription_RXID = ?"
			);
			fillCountPS.setInt(1, p.getRxid());
			ResultSet fillCountRS = fillCountPS.executeQuery();
			int fillCount = 0;
			if (fillCountRS.next()) {
				fillCount = fillCountRS.getInt("fill_count");
			}

			// 4. Ensure Refills Have Not Exceeded the Limit
			if (fillCount >= p.getRefills()) {
				model.addAttribute("message", "No refills remaining.");
				return "prescription_fill";
			}

			// 5. Calculate Cost
			PreparedStatement costPS = con.prepareStatement(
					"SELECT price_per_unit FROM pharmacy_drug_cost WHERE pharmacy_ID = ? AND drug_ID = ?"
			);
			costPS.setInt(1, p.getPharmacyID());
			costPS.setInt(2, Integer.parseInt(p.getDrugName()));
			ResultSet costRS = costPS.executeQuery();
			if (costRS.next()) {
				p.setCost(String.valueOf(costRS.getDouble("price_per_unit")));
			} else {
				model.addAttribute("message", "Error: Drug price not found.");
				return "prescription_fill";
			}

			// 6. Insert New Fill Record
			PreparedStatement insertFillPS = con.prepareStatement(
					"INSERT INTO prescription_filled (prescription_RXID, pharmacy_ID, date_filled, cost) VALUES (?, ?, ?, ?)"
			);
			insertFillPS.setInt(1, p.getRxid());
			insertFillPS.setInt(2, p.getPharmacyID());
			insertFillPS.setDate(3, Date.valueOf(LocalDate.now()));
			insertFillPS.setDouble(4, Double.parseDouble(p.getCost()));
			insertFillPS.executeUpdate();

			System.out.println("Prescription fill recorded successfully for RXID: " + p.getRxid());

			// 7. Show Updated Prescription
			model.addAttribute("message", "Prescription filled successfully.");
			model.addAttribute("prescription", p);
			return "prescription_show";

		} catch (SQLException e) {
			System.out.println("SQL Error: " + e.getMessage());
			model.addAttribute("message", "SQL Error: " + e.getMessage());
			return "prescription_fill";
		}
	}

	private Connection getConnection() throws SQLException {
		return jdbcTemplate.getDataSource().getConnection();
	}
}