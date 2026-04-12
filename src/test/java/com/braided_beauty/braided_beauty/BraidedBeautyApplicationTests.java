package com.braided_beauty.braided_beauty;

import com.braided_beauty.braided_beauty.models.ScheduleCalendar;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.metamodel.ManagedType;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;

import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("local")
class BraidedBeautyApplicationTests {

	@Autowired
	private EntityManagerFactory entityManagerFactory;

	@Test
	void contextLoads() {
	}

	@Test
	void scheduleCalendarMetamodelUsesDatetimeBookingFieldsOnly() {
		ManagedType<ScheduleCalendar> managedType = entityManagerFactory.getMetamodel().managedType(ScheduleCalendar.class);
		Set<String> attrs = managedType.getAttributes().stream()
				.map(a -> a.getName())
				.collect(Collectors.toSet());

		assertTrue(attrs.contains("bookingOpenAt"));
		assertTrue(attrs.contains("bookingCloseAt"));
		assertFalse(attrs.contains("bookingOpenDate"));
		assertFalse(attrs.contains("bookingCloseDate"));
	}

}
