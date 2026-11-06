package co.sierraalta.production.history.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Lote de observaciones cargado de una vez por un usuario. Queda como registro de auditoría de la carga
 * y su id es la versión de datos de la serie. No se modifica después de creado.
 */
@Entity
@Table(name = "carga")
public class DataLoad {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "serie_id", nullable = false, updatable = false)
	private Series series;

	/** Autor de la carga. Se guarda solo el id para no acoplar este módulo a las entidades de {@code auth}. */
	@Column(name = "usuario_id", nullable = false, updatable = false)
	private Long userId;

	@Column(name = "fecha", nullable = false, updatable = false)
	private Instant loadedAt;

	@Column(name = "cantidad", nullable = false, updatable = false)
	private int count;

	@Column(name = "periodo_desde", nullable = false, updatable = false)
	private LocalDate firstPeriod;

	@Column(name = "periodo_hasta", nullable = false, updatable = false)
	private LocalDate lastPeriod;

	protected DataLoad() {
		// Requerido por JPA.
	}

	public DataLoad(Series series, Long userId, int count, YearMonth firstPeriod, YearMonth lastPeriod) {
		this.series = series;
		this.userId = userId;
		this.loadedAt = Instant.now();
		this.count = count;
		this.firstPeriod = firstPeriod.atDay(1);
		this.lastPeriod = lastPeriod.atDay(1);
	}

	public Long getId() {
		return id;
	}

	public Long getUserId() {
		return userId;
	}

	public Instant getLoadedAt() {
		return loadedAt;
	}

	public int getCount() {
		return count;
	}

	public YearMonth getFirstPeriod() {
		return YearMonth.from(firstPeriod);
	}

	public YearMonth getLastPeriod() {
		return YearMonth.from(lastPeriod);
	}
}
