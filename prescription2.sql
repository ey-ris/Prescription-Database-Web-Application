-- MySQL Workbench Forward Engineering

SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0;
SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0;
SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION';

-- -----------------------------------------------------
-- Schema prescription
-- -----------------------------------------------------
DROP SCHEMA IF EXISTS `prescription` ;

-- -----------------------------------------------------
-- Schema prescription
-- -----------------------------------------------------
CREATE SCHEMA IF NOT EXISTS `prescription` DEFAULT CHARACTER SET utf8 ;
USE `prescription` ;

-- -----------------------------------------------------
-- Table `prescription`.`doctor`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `prescription`.`doctor` (
  `ID` INT NOT NULL AUTO_INCREMENT,
  `ssn` CHAR(9) NOT NULL,
  `last_name` VARCHAR(45) NOT NULL,
  `first_name` VARCHAR(45) NOT NULL,
  `specialty` VARCHAR(45) NULL,
  `practice_since` INT NULL,
  PRIMARY KEY (`ID`),
  UNIQUE INDEX `social_security_UNIQUE` (`ssn` ASC) VISIBLE)
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `prescription`.`patient`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `prescription`.`patient` (
  `ID` INT NOT NULL AUTO_INCREMENT,
  `ssn` CHAR(9) NOT NULL,
  `first_name` VARCHAR(45) NOT NULL,
  `last_name` VARCHAR(45) NOT NULL,
  `birth_date` DATE NOT NULL,
  `street` VARCHAR(60) NULL,
  `city` VARCHAR(45) NULL,
  `state` CHAR(2) NULL,
  `zipcode` CHAR(5) NULL,
  `doctor_ID` INT NULL,
  PRIMARY KEY (`ID`),
  UNIQUE INDEX `ID_UNIQUE` (`ID` ASC) VISIBLE,
  UNIQUE INDEX `ssn_UNIQUE` (`ssn` ASC) VISIBLE,
  INDEX `fk_patient_doctor_idx` (`doctor_ID` ASC) VISIBLE,
  CONSTRAINT `fk_patient_doctor`
    FOREIGN KEY (`doctor_ID`)
    REFERENCES `prescription`.`doctor` (`ID`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `prescription`.`drug`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `prescription`.`drug` (
  `ID` INT NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(45) NOT NULL,
  PRIMARY KEY (`ID`),
  UNIQUE INDEX `name_UNIQUE` (`name` ASC) VISIBLE)
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `prescription`.`prescription`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `prescription`.`prescription` (
  `RXID` INT NOT NULL AUTO_INCREMENT,
  `doctor_ID` INT NOT NULL,
  `patient_ID` INT NOT NULL,
  `drug_ID` INT NOT NULL,
  `drug_quantity` INT UNSIGNED NOT NULL,
  `max_number_refill` INT UNSIGNED NOT NULL,
  `date_created` DATE NOT NULL,
  PRIMARY KEY (`RXID`),
  INDEX `fk_prescription_patient1_idx` (`patient_ID` ASC) VISIBLE,
  UNIQUE INDEX `RXID_UNIQUE` (`RXID` ASC) VISIBLE,
  INDEX `fk_prescription_drug_idx` (`drug_ID` ASC) VISIBLE,
  CONSTRAINT `doctor_id`
    FOREIGN KEY (`doctor_ID`)
    REFERENCES `prescription`.`doctor` (`ID`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
  CONSTRAINT `fk_prescription_patient1`
    FOREIGN KEY (`patient_ID`)
    REFERENCES `prescription`.`patient` (`ID`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
  CONSTRAINT `fk_prescription_drug`
    FOREIGN KEY (`drug_ID`)
    REFERENCES `prescription`.`drug` (`ID`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `prescription`.`pharmacy`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `prescription`.`pharmacy` (
  `ID` INT NOT NULL AUTO_INCREMENT,
  `address` VARCHAR(60) NOT NULL,
  `phone` VARCHAR(10) NOT NULL,
  `name` VARCHAR(45) NOT NULL,
  PRIMARY KEY (`ID`),
  UNIQUE INDEX `address_UNIQUE` (`address` ASC) VISIBLE)
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `prescription`.`pharmacy_drug_cost`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `prescription`.`pharmacy_drug_cost` (
  `pharmacy_ID` INT NOT NULL,
  `drug_ID` INT NOT NULL,
  `unit_per_sale` INT UNSIGNED NOT NULL,
  `price_per_unit` DECIMAL(5,2) UNSIGNED NOT NULL,
  PRIMARY KEY (`pharmacy_ID`, `drug_ID`),
  INDEX `fk_pharmacy_has_drug_drug1_idx` (`drug_ID` ASC) VISIBLE,
  INDEX `fk_pharmacy_has_drug_pharmacy1_idx` (`pharmacy_ID` ASC) VISIBLE,
  CONSTRAINT `fk_pharmacy_has_drug_pharmacy1`
    FOREIGN KEY (`pharmacy_ID`)
    REFERENCES `prescription`.`pharmacy` (`ID`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
  CONSTRAINT `fk_pharmacy_has_drug_drug1`
    FOREIGN KEY (`drug_ID`)
    REFERENCES `prescription`.`drug` (`ID`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `prescription`.`prescription_filled`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `prescription`.`prescription_filled` (
  `fill_no` INT NOT NULL AUTO_INCREMENT,
  `prescription_RXID` INT NOT NULL,
  `pharmacy_ID` INT NOT NULL,
  `date_filled` DATE NOT NULL,
  `cost` DECIMAL(5,2) NOT NULL,
  PRIMARY KEY (`fill_no`, `prescription_RXID`),
  INDEX `fk_prescription_filled_pharmacy_idx` (`pharmacy_ID` ASC) VISIBLE,
  CONSTRAINT `fk_prescription_filled_prescription1`
    FOREIGN KEY (`prescription_RXID`)
    REFERENCES `prescription`.`prescription` (`RXID`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
  CONSTRAINT `fk_prescription_filled_pharmacy`
    FOREIGN KEY (`pharmacy_ID`)
    REFERENCES `prescription`.`pharmacy` (`ID`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB;


SET SQL_MODE=@OLD_SQL_MODE;
SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS;
SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS;
USE `prescription`;

DELIMITER $$

CREATE DEFINER = CURRENT_USER TRIGGER `prescription`.`prescription_filled_BEFORE_INSERT` BEFORE INSERT ON `prescription_filled` FOR EACH ROW
BEGIN
    DECLARE refill_count INT;
    DECLARE max_refills INT;

    SELECT COUNT(*), p.max_number_refill INTO refill_count, max_refills
    FROM prescription_filled pf
    INNER JOIN prescription p ON pf.prescription_RXID = p.RXID
    WHERE pf.prescription_RXID = NEW.prescription_RXID
    GROUP BY p.RXID; -- Crucial: Add GROUP BY clause

    IF refill_count >= max_refills THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = "prescription complete";
    END IF;
END$$

DELIMITER ;
