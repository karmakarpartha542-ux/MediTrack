package com.meditrack.service;

import com.meditrack.model.billing.Bill;
import com.meditrack.model.billing.BillItem;
import com.meditrack.model.room.Room;
import com.meditrack.repository.FileBillingRepository;
import com.meditrack.repository.FileRoomRepository;
import com.meditrack.util.JsonUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Service managing billing invoices, invoice generation, discount calculations,
 * and outstanding payment metrics.
 */
public class BillingService {

    private final FileBillingRepository billingRepo;
    private final FileRoomRepository roomRepo;

    public BillingService(FileBillingRepository billingRepo, FileRoomRepository roomRepo) {
        this.billingRepo = billingRepo;
        this.roomRepo = roomRepo;
    }

    /**
     * Generates a patient invoice combining room occupancy and consultation charges.
     *
     * ========================================================================
     * OOP CONCEPT: POLYMORPHISM
     * ========================================================================
     * POLYMORPHISM: room.calculateCharges() works for GeneralWard, Cabin, and ICU
     * without type-checking.
     *
     * At compile-time, the code only knows it has a reference of type {@link Room}.
     * At runtime, the JVM uses dynamic method dispatch to invoke the concrete
     * implementation:
     * - {@code GeneralWard.calculateCharges()} for standard ward beds
     * - {@code Cabin.calculateCharges()} factoring in AC surcharges
     * - {@code ICU.calculateCharges()} factoring in life-support ventilator fees
     *
     * This avoids brittle {@code if (room instanceof ...)} branching and allows
     * new room types to be added seamlessly without modifying billing logic.
     * ========================================================================
     *
     * @param patientId         identifier of the patient
     * @param patientName       full name of the patient
     * @param roomId            identifier of the occupied room (nullable)
     * @param daysStayed        number of days accommodated
     * @param consultationFee   consultation charge
     * @param doctorDescription description of doctor consultation
     * @return generated and persisted Bill
     */
    public Bill generateBill(String patientId, String patientName, String roomId,
                             int daysStayed, double consultationFee, String doctorDescription) {
        List<BillItem> items = new ArrayList<>();

        if (roomId != null && !roomId.trim().isEmpty() && !roomId.equalsIgnoreCase("null")) {
            Optional<Room> roomOpt = roomRepo.findById(roomId);
            if (roomOpt.isPresent()) {
                Room room = roomOpt.get();
                int days = Math.max(1, daysStayed);
                // Polymorphic charge calculation!
                double roomCharges = room.calculateCharges(days);
                String itemDesc = room.getRoomType() + " (Room " + room.getRoomNumber() + ") - " + days + " day(s)";
                items.add(new BillItem(itemDesc, roomCharges));
            }
        }

        if (consultationFee > 0.0) {
            String feeDesc = (doctorDescription != null && !doctorDescription.trim().isEmpty())
                    ? doctorDescription.trim()
                    : "Doctor Consultation Fee";
            items.add(new BillItem(feeDesc, consultationFee));
        }

        String billId = JsonUtils.generateId("BIL");
        Bill bill = new Bill(billId, patientId, patientName, items, 0.0, false,
                LocalDate.now().toString(), "Hospital Services & Accommodation Invoice");
        bill.recalculateTotal();

        billingRepo.save(bill);
        return bill;
    }

    /**
     * Retrieves all invoices.
     *
     * @return list of bills
     */
    public List<Bill> getAllBills() {
        return billingRepo.findAll();
    }

    /**
     * Retrieves all pending/unpaid invoices.
     *
     * @return list of unpaid bills
     */
    public List<Bill> getPendingBills() {
        return billingRepo.findUnpaid();
    }

    /**
     * Finds a bill by ID.
     *
     * @param id bill identifier
     * @return Optional containing Bill if found
     */
    public Optional<Bill> getBillById(String id) {
        return billingRepo.findById(id);
    }

    /**
     * Marks an invoice as settled and paid.
     *
     * @param id bill ID
     * @return true if marked paid, false if bill not found
     */
    public boolean markBillPaid(String id) {
        Optional<Bill> opt = billingRepo.findById(id);
        if (opt.isPresent()) {
            Bill bill = opt.get();
            bill.markPaid();
            billingRepo.update(bill);
            return true;
        }
        return false;
    }

    /**
     * Retrieves all invoices for a given patient.
     *
     * @param patientId patient identifier
     * @return list of matching bills
     */
    public List<Bill> getBillsByPatient(String patientId) {
        return billingRepo.findByPatientId(patientId);
    }

    /**
     * Calculates the cumulative monetary total of all unpaid hospital bills.
     *
     * @return total pending amount
     */
    public double getTotalPendingAmount() {
        return billingRepo.findUnpaid().stream()
                .mapToDouble(Bill::getTotalAmount)
                .sum();
    }
}
