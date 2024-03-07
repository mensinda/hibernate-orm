package org.hibernate.orm.test.locking;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.LockModeType;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Version;
import org.hibernate.Hibernate;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@JiraKey("HHH-?????")
@Jpa(
		annotatedClasses = {
				LockFindAndLockReferencedTest.MainEntity.class,
				LockFindAndLockReferencedTest.ReferencedEntity.class
		}
)
public class LockFindAndLockReferencedTest {

	@BeforeAll
	public void setUp(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					final ReferencedEntity e1 = new ReferencedEntity( 0L, "lazy" );
					final ReferencedEntity e2 = new ReferencedEntity( 1L, "eager" );
					entityManager.persist( e1 );
					entityManager.persist( e2 );
					final MainEntity e3 = new MainEntity( 0L, e1, e2 );
					entityManager.persist( e3 );
				}
		);
	}

	@Test
	public void testFindAndLockAfterLock(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
                    // First find and lock the main entity
					MainEntity m = entityManager.find( MainEntity.class, 0L, LockModeType.PESSIMISTIC_WRITE );
					assertNotNull( m );
					ReferencedEntity lazyReference = m.referencedLazy();
					ReferencedEntity eagerReference = m.referencedEager();
					assertNotNull( lazyReference );
					assertNotNull( eagerReference );
					assertFalse( Hibernate.isInitialized( lazyReference ) );

					// Then find and lock the referenced entity
                    ReferencedEntity lazyEntity = entityManager.find( ReferencedEntity.class, 0L, LockModeType.PESSIMISTIC_WRITE );
                    ReferencedEntity eagerEntity = entityManager.find( ReferencedEntity.class, 1L, LockModeType.PESSIMISTIC_WRITE );
                    assertNotNull( lazyEntity );
                    assertNotNull( eagerEntity );

					assertEquals( LockModeType.PESSIMISTIC_WRITE, entityManager.getLockMode( lazyEntity ) );
					assertEquals( LockModeType.PESSIMISTIC_WRITE, entityManager.getLockMode( eagerEntity ) );
				} );
	}

	@Entity(name = "MainEntity")
	public static class MainEntity {
		@Id
		private Long id;

        @Version
        private long tanum;

		private String name;

		@OneToOne(targetEntity = ReferencedEntity.class, fetch = FetchType.LAZY)
		@JoinColumn(name = "LAZY_COLUMN")
		private ReferencedEntity referencedLazy;

		@OneToOne(targetEntity = ReferencedEntity.class, fetch = FetchType.EAGER)
		@JoinColumn(name = "EAGER_COLUMN")
		private ReferencedEntity referencedEager;

		protected MainEntity() {
		}

		public MainEntity(Long id, ReferencedEntity lazy, ReferencedEntity eager) {
			this.id = id;
			this.referencedLazy = lazy;
			this.referencedEager = eager;
		}

		public ReferencedEntity referencedLazy() {
			return referencedLazy;
		}

		public ReferencedEntity referencedEager() {
			return referencedEager;
		}
	}

	@Entity(name = "ReferencedEntity")
	public static class ReferencedEntity {

		@Id
		private Long id;

        @Version
        private long tanum;

		private String status;

		protected ReferencedEntity() {
		}

		public ReferencedEntity(Long id, String status) {
			this.id = id;
			this.status = status;
		}

		public String status() {
			return status;
		}
	}

}
