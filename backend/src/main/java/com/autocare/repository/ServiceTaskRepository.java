package com.autocare.repository;

import com.autocare.domain.ServiceTask;
import com.autocare.domain.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA repository for ServiceTask entity.
 * Handles job cards, mechanic assignments, and task status queries.
 */
@Repository
public interface ServiceTaskRepository extends JpaRepository<ServiceTask, String> {

    /**
     * Finds all service tasks with the specified lifecycle status.
     *
     * @param status task status (PENDING, IN_PROGRESS, COMPLETED)
     * @return list of service tasks matching the status
     */
    List<ServiceTask> findByStatus(TaskStatus status);

    /**
     * Finds all service tasks assigned to a particular mechanic.
     *
     * @param mechanicId ID of the assigned mechanic
     * @return list of service tasks assigned to the mechanic
     */
    @Query("SELECT t FROM ServiceTask t WHERE t.assignedMechanic.id = :mechanicId")
    List<ServiceTask> findByAssignedMechanicEmployeeId(@Param("mechanicId") String mechanicId);

    /**
     * Finds all service tasks associated with a given invoice.
     *
     * @param invoiceId ID of the invoice
     * @return list of service tasks on the invoice
     */
    @Query("SELECT t FROM ServiceTask t WHERE t.invoice.invoiceId = :invoiceId")
    List<ServiceTask> findByInvoiceId(@Param("invoiceId") String invoiceId);

    /**
     * Filters service tasks by optional status and optional mechanic ID.
     *
     * @param status     optional status filter
     * @param mechanicId optional mechanic ID filter
     * @return list of matching service tasks
     */
    @Query("SELECT t FROM ServiceTask t WHERE " +
            "(:status IS NULL OR t.status = :status) AND " +
            "(:mechanicId IS NULL OR t.assignedMechanic.id = :mechanicId)")
    List<ServiceTask> findByFilter(@Param("status") TaskStatus status, @Param("mechanicId") String mechanicId);
}
