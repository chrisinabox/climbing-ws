package com.buldreinfo.jersey.jaxb.db;

import java.awt.Point;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;

import com.buldreinfo.jersey.jaxb.helpers.Auth0Profile;
import com.buldreinfo.jersey.jaxb.helpers.GeoHelper;
import com.buldreinfo.jersey.jaxb.helpers.GlobalFunctions;
import com.buldreinfo.jersey.jaxb.helpers.GradeConverter;
import com.buldreinfo.jersey.jaxb.helpers.MetaHelper;
import com.buldreinfo.jersey.jaxb.helpers.Setup;
import com.buldreinfo.jersey.jaxb.helpers.Setup.GRADE_SYSTEM;
import com.buldreinfo.jersey.jaxb.helpers.TimeAgo;
import com.buldreinfo.jersey.jaxb.model.Activity;
import com.buldreinfo.jersey.jaxb.model.Administrator;
import com.buldreinfo.jersey.jaxb.model.Approach;
import com.buldreinfo.jersey.jaxb.model.Area;
import com.buldreinfo.jersey.jaxb.model.Area.AreaSectorOrder;
import com.buldreinfo.jersey.jaxb.model.Comment;
import com.buldreinfo.jersey.jaxb.model.CompassDirection;
import com.buldreinfo.jersey.jaxb.model.Coordinates;
import com.buldreinfo.jersey.jaxb.model.Dangerous;
import com.buldreinfo.jersey.jaxb.model.FaAid;
import com.buldreinfo.jersey.jaxb.model.FaUser;
import com.buldreinfo.jersey.jaxb.model.Frontpage;
import com.buldreinfo.jersey.jaxb.model.Grade;
import com.buldreinfo.jersey.jaxb.model.GradeDistribution;
import com.buldreinfo.jersey.jaxb.model.Media;
import com.buldreinfo.jersey.jaxb.model.MediaInfo;
import com.buldreinfo.jersey.jaxb.model.MediaMetadata;
import com.buldreinfo.jersey.jaxb.model.MediaSvgElement;
import com.buldreinfo.jersey.jaxb.model.NewMedia;
import com.buldreinfo.jersey.jaxb.model.PermissionUser;
import com.buldreinfo.jersey.jaxb.model.Problem;
import com.buldreinfo.jersey.jaxb.model.Problem.ProblemComment;
import com.buldreinfo.jersey.jaxb.model.Problem.ProblemSection;
import com.buldreinfo.jersey.jaxb.model.ProblemArea;
import com.buldreinfo.jersey.jaxb.model.Profile;
import com.buldreinfo.jersey.jaxb.model.ProfileMedia;
import com.buldreinfo.jersey.jaxb.model.ProfileStatistics;
import com.buldreinfo.jersey.jaxb.model.ProfileTodo;
import com.buldreinfo.jersey.jaxb.model.PublicAscent;
import com.buldreinfo.jersey.jaxb.model.Redirect;
import com.buldreinfo.jersey.jaxb.model.Search;
import com.buldreinfo.jersey.jaxb.model.Sector;
import com.buldreinfo.jersey.jaxb.model.Sector.SectorProblemOrder;
import com.buldreinfo.jersey.jaxb.model.SectorProblem;
import com.buldreinfo.jersey.jaxb.model.Site;
import com.buldreinfo.jersey.jaxb.model.Svg;
import com.buldreinfo.jersey.jaxb.model.Tick;
import com.buldreinfo.jersey.jaxb.model.TickRepeat;
import com.buldreinfo.jersey.jaxb.model.Ticks;
import com.buldreinfo.jersey.jaxb.model.Todo;
import com.buldreinfo.jersey.jaxb.model.Top;
import com.buldreinfo.jersey.jaxb.model.Trash;
import com.buldreinfo.jersey.jaxb.model.Type;
import com.buldreinfo.jersey.jaxb.model.TypeNumTicked;
import com.buldreinfo.jersey.jaxb.model.UserRegion;
import com.buldreinfo.jersey.jaxb.model.UserSearch;
import com.buldreinfo.jersey.jaxb.model.v1.V1Region;
import com.buldreinfo.jersey.jaxb.util.excel.ExcelReport;
import com.buldreinfo.jersey.jaxb.util.excel.ExcelReport.SheetHyperlink;
import com.buldreinfo.jersey.jaxb.util.excel.ExcelReport.SheetWriter;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Stopwatch;
import com.google.common.base.Strings;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

/**
 * @author <a href="mailto:jostein.oygarden@gmail.com">Jostein Oeygarden</a>
 */
public class BuldreinfoRepository {
	private static final String ACTIVITY_TYPE_FA = "FA";
	private static final String ACTIVITY_TYPE_MEDIA = "MEDIA";
	private static final String ACTIVITY_TYPE_GUESTBOOK = "GUESTBOOK";
	private static final String ACTIVITY_TYPE_TICK = "TICK";
	private static final String ACTIVITY_TYPE_TICK_REPEAT = "TICK_REPEAT";
	private static Logger logger = LogManager.getLogger();
	private final DbConnection c;

	private final Gson gson = new Gson();

	protected BuldreinfoRepository(DbConnection c) {
		this.c = c;
	}

	public void addProblemMedia(int authUserId, Problem p, FormDataMultiPart multiPart) throws NoSuchAlgorithmException, SQLException, IOException, InterruptedException {
		Preconditions.checkArgument(authUserId != -1, "Insufficient permissions");
		for (NewMedia m : p.getNewMedia()) {
			final int idSector = 0;
			final int idArea = 0;
			final int idGuestbook = 0;
			addNewMedia(authUserId, p.getId(), m.getPitch(), m.isTrivia(), idSector, idArea, idGuestbook, m, multiPart);
		}
		fillActivity(p.getId());
	}

	public void deleteMedia(int authUserId, int id) throws SQLException {
		List<Integer> idProblems = new ArrayList<>();
		try (PreparedStatement ps = c.getConnection().prepareStatement("SELECT problem_id FROM media_problem WHERE media_id=?")) {
			ps.setInt(1, id);
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					idProblems.add(rst.getInt("problem_id"));
				}
			}
		}

		boolean ok = false;
		try (PreparedStatement ps = c.getConnection().prepareStatement("SELECT ur.admin_write, ur.superadmin_write FROM ((((((area a INNER JOIN sector s ON a.id=s.area_id) INNER JOIN user_region ur ON (a.region_id=ur.region_id AND ur.user_id=?)) LEFT JOIN media_area ma ON (a.id=ma.area_id AND ma.media_id=?) LEFT JOIN media_sector ms ON (s.id=ms.sector_id AND ms.media_id=?)) LEFT JOIN problem p ON s.id=p.sector_id) LEFT JOIN media_problem mp ON (p.id=mp.problem_id AND mp.media_id=?) LEFT JOIN guestbook g ON (p.id=g.problem_id)) LEFT JOIN media_guestbook mg ON (g.id=mg.guestbook_id AND mg.media_id=?)) WHERE ma.media_id IS NOT NULL OR ms.media_id IS NOT NULL OR mp.media_id IS NOT NULL OR mg.media_id IS NOT NULL GROUP BY ur.admin_write, ur.superadmin_write")) {
			ps.setInt(1, authUserId);
			ps.setInt(2, id);
			ps.setInt(3, id);
			ps.setInt(4, id);
			ps.setInt(5, id);
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					ok = rst.getBoolean("admin_write") || rst.getBoolean("superadmin_write");
				}
			}
		}
		Preconditions.checkArgument(ok, "Insufficient permissions");
		try (PreparedStatement ps = c.getConnection().prepareStatement("UPDATE media SET deleted_user_id=?, deleted_timestamp=NOW() WHERE id=?")) {
			ps.setInt(1, authUserId);
			ps.setInt(2, id);
			ps.execute();
		}

		for (int idProblem : idProblems) {
			fillActivity(idProblem);
		}
	}

	public void ensureCoordinatesInDbWithElevationAndId(List<Coordinates> coordinates) throws SQLException {
		if (coordinates != null && !coordinates.isEmpty()) {
			// First round coordinates to 10 digits (to match database type)
			coordinates.forEach(coord -> coord.roundCoordinatesToMaximum10digitsAfterComma());
			// Ensure coordinates exists in db
			try (PreparedStatement ps = c.getConnection().prepareStatement("INSERT INTO coordinates (latitude, longitude, elevation, elevation_source) VALUES (?, ?, ?, ?) ON DUPLICATE KEY UPDATE elevation=?, elevation_source=?")) {
				for (Coordinates coord : coordinates) {
					ps.setDouble(1, coord.getLatitude());
					ps.setDouble(2, coord.getLongitude());
					// Use elevation from GPX if available, this has better quality compared with Google Elevation API
					if (coord.getElevation() > 0 && coord.getElevationSource() != null && coord.getElevationSource().equals(Coordinates.ELEVATION_SOURCE_GPX)) {
						ps.setDouble(3, coord.getElevation());
						ps.setString(4, coord.getElevationSource());
						ps.setDouble(5, coord.getElevation());
						ps.setString(6, coord.getElevationSource());
					}
					else {
						ps.setNull(3, Types.DOUBLE);
						ps.setNull(4, Types.VARCHAR);
						ps.setNull(5, Types.DOUBLE);
						ps.setNull(6, Types.VARCHAR);
					}
					ps.addBatch();
				}
				ps.executeBatch();
			}
		}
		// Fill missing elevations in db
		List<Coordinates> coordinatesMissingElevation = new ArrayList<>();
		try (PreparedStatement ps = c.getConnection().prepareStatement("SELECT id, latitude, longitude, elevation, elevation_source FROM coordinates WHERE elevation IS NULL")) {
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					int id = rst.getInt("id");
					double latitude = rst.getDouble("latitude");
					double longitude = rst.getDouble("longitude");
					double elevation = rst.getDouble("elevation");
					String elevationSource = rst.getString("elevation_source");
					coordinatesMissingElevation.add(new Coordinates(id, latitude, longitude, elevation, elevationSource));
				}
			}
		}
		if (!coordinatesMissingElevation.isEmpty()) {
			try {
				GeoHelper.fillElevations(coordinatesMissingElevation);
				try (PreparedStatement ps = c.getConnection().prepareStatement("UPDATE coordinates SET elevation=?, elevation_source=? WHERE id=?")) {
					for (Coordinates coord : coordinatesMissingElevation) {
						ps.setDouble(1, coord.getElevation());
						ps.setString(2, coord.getElevationSource());
						ps.setDouble(3, coord.getId());
						ps.addBatch();
					}
					ps.executeBatch();
				}
			} catch (IOException e) {
				logger.warn(e.getMessage(), e);
			}
		}
		if (coordinates != null && !coordinates.isEmpty()) {
			// Fetch correct id's and elevation's (id can be wrong in coordinates - user might have changed latitude/longitude on existing id)
			for (Coordinates coord : coordinates) {
				try (PreparedStatement ps = c.getConnection().prepareStatement("SELECT id, elevation, elevation_source FROM coordinates WHERE latitude=? AND longitude=?")) {
					ps.setDouble(1, coord.getLatitude());
					ps.setDouble(2, coord.getLongitude());
					try (ResultSet rst = ps.executeQuery()) {
						while (rst.next()) {
							int id = rst.getInt("id");
							double elevation = rst.getDouble("elevation");
							String elevationSource = rst.getString("elevation_source");
							coord.setId(id);
							coord.setElevation(elevation, elevationSource);
						}
					}
				}
			}
		}
	}

	public void fillActivity(int idProblem) throws SQLException {
		/**
		 * Delete existing activities on problem
		 */
		try (PreparedStatement ps = c.getConnection().prepareStatement("DELETE FROM activity WHERE problem_id=?")) {
			ps.setInt(1, idProblem);
			ps.execute();
		}

		/**
		 * FA
		 */
		LocalDateTime problemActivityTimestamp = null;
		boolean exists = false;
		try (PreparedStatement ps = c.getConnection().prepareStatement("SELECT fa_date, last_updated FROM problem WHERE id=?")) {
			ps.setInt(1, idProblem);
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					exists = true;
					Timestamp faDate = rst.getTimestamp("fa_date");
					Timestamp lastUpdated = rst.getTimestamp("last_updated");
					if (faDate != null && lastUpdated != null) {
						Calendar faCal = Calendar.getInstance();
						faCal.setTimeInMillis(faDate.getTime());
						Calendar luCal = Calendar.getInstance();
						luCal.setTimeInMillis(lastUpdated.getTime());
						problemActivityTimestamp = LocalDateTime.of(faCal.get(Calendar.YEAR), faCal.get(Calendar.MONTH)+1, faCal.get(Calendar.DAY_OF_MONTH), luCal.get(Calendar.HOUR_OF_DAY), luCal.get(Calendar.MINUTE), luCal.get(Calendar.SECOND));
					}
					else if (faDate != null) {
						Calendar faCal = Calendar.getInstance();
						faCal.setTimeInMillis(faDate.getTime());
						problemActivityTimestamp = LocalDateTime.of(faCal.get(Calendar.YEAR), faCal.get(Calendar.MONTH)+1, faCal.get(Calendar.DAY_OF_MONTH), 0, 0, 0);
					}
				}
			}
		}
		if (!exists) {
			return;
		}
		try (PreparedStatement psAddActivity = c.getConnection().prepareStatement("INSERT INTO activity (activity_timestamp, type, problem_id, media_id, user_id, guestbook_id, tick_repeat_id) VALUES (?, ?, ?, ?, ?, ?, ?)")) {
			psAddActivity.setTimestamp(1, problemActivityTimestamp == null? new Timestamp(0) : Timestamp.valueOf(problemActivityTimestamp));
			psAddActivity.setString(2, ACTIVITY_TYPE_FA);
			psAddActivity.setInt(3, idProblem);
			psAddActivity.setNull(4, Types.INTEGER);
			psAddActivity.setNull(5, Types.INTEGER);
			psAddActivity.setNull(6, Types.INTEGER);
			psAddActivity.setNull(7, Types.INTEGER);
			psAddActivity.addBatch();


			/**
			 * Media
			 */
			try (PreparedStatement ps = c.getConnection().prepareStatement("SELECT m.id, m.date_created FROM media_problem mp, media m WHERE mp.problem_id=? AND mp.media_id=m.id AND m.deleted_timestamp IS NULL ORDER BY m.date_created DESC")) {
				ps.setInt(1, idProblem);
				try (ResultSet rst = ps.executeQuery()) {
					LocalDateTime useMediaActivityTimestamp = null;
					while (rst.next()) {
						int id = rst.getInt("id");
						Timestamp ts = rst.getTimestamp("date_created");
						LocalDateTime mediaActivityTimestamp = ts == null? null : ts.toLocalDateTime();
						if (mediaActivityTimestamp == null || (problemActivityTimestamp != null && Math.abs(ChronoUnit.DAYS.between(problemActivityTimestamp, mediaActivityTimestamp)) <= 7)) {
							useMediaActivityTimestamp = problemActivityTimestamp;
						}
						else if (useMediaActivityTimestamp == null || Math.abs(ChronoUnit.DAYS.between(useMediaActivityTimestamp, mediaActivityTimestamp)) > 7) {
							useMediaActivityTimestamp = mediaActivityTimestamp;
						}
						psAddActivity.setTimestamp(1, useMediaActivityTimestamp == null? new Timestamp(0) : Timestamp.valueOf(useMediaActivityTimestamp));
						psAddActivity.setString(2, ACTIVITY_TYPE_MEDIA);
						psAddActivity.setInt(3, idProblem);
						psAddActivity.setInt(4, id);
						psAddActivity.setNull(5, Types.INTEGER);
						psAddActivity.setNull(6, Types.INTEGER);
						psAddActivity.setNull(7, Types.INTEGER);
						psAddActivity.addBatch();
					}
				}
			}

			/**
			 * Tick
			 */
			try (PreparedStatement ps = c.getConnection().prepareStatement("SELECT user_id, date, created FROM tick WHERE problem_id=? ORDER BY date, created")) {
				ps.setInt(1, idProblem);
				try (ResultSet rst = ps.executeQuery()) {
					while (rst.next()) {
						int userId = rst.getInt("user_id");
						LocalDateTime tickActivityTimestamp = null;
						Timestamp tickDate = rst.getTimestamp("date");
						Timestamp tickCreated = rst.getTimestamp("created");
						if (tickDate != null && tickCreated != null) {
							Calendar tickCal = Calendar.getInstance();
							tickCal.setTimeInMillis(tickDate.getTime());
							Calendar createdCal = Calendar.getInstance();
							createdCal.setTimeInMillis(tickCreated.getTime());
							tickActivityTimestamp = LocalDateTime.of(tickCal.get(Calendar.YEAR), tickCal.get(Calendar.MONTH)+1, tickCal.get(Calendar.DAY_OF_MONTH), createdCal.get(Calendar.HOUR_OF_DAY), createdCal.get(Calendar.MINUTE), createdCal.get(Calendar.SECOND));
						}
						else if (tickDate != null) {
							Calendar tickCal = Calendar.getInstance();
							tickCal.setTimeInMillis(tickDate.getTime());
							tickActivityTimestamp = LocalDateTime.of(tickCal.get(Calendar.YEAR), tickCal.get(Calendar.MONTH)+1, tickCal.get(Calendar.DAY_OF_MONTH), 0, 0, 0);
						}
						psAddActivity.setTimestamp(1, tickActivityTimestamp == null? new Timestamp(0) : Timestamp.valueOf(tickActivityTimestamp));
						psAddActivity.setString(2, ACTIVITY_TYPE_TICK);
						psAddActivity.setInt(3, idProblem);
						psAddActivity.setNull(4, Types.INTEGER);
						psAddActivity.setInt(5, userId);
						psAddActivity.setNull(6, Types.INTEGER);
						psAddActivity.setNull(7, Types.INTEGER);
						psAddActivity.addBatch();
					}
				}
			}

			/**
			 * Tick repeat
			 */
			try (PreparedStatement ps = c.getConnection().prepareStatement("SELECT r.id, t.user_id, r.date, r.created FROM tick t, tick_repeat r WHERE t.problem_id=? AND t.id=r.tick_id ORDER BY r.tick_id, r.date, r.id")) {
				ps.setInt(1, idProblem);
				try (ResultSet rst = ps.executeQuery()) {
					while (rst.next()) {
						int id = rst.getInt("id");
						int userId = rst.getInt("user_id");
						LocalDateTime tickRepeatActivityTimestamp = null;
						Timestamp tickDate = rst.getTimestamp("date");
						Timestamp tickCreated = rst.getTimestamp("created");
						if (tickDate != null && tickCreated != null) {
							Calendar tickCal = Calendar.getInstance();
							tickCal.setTimeInMillis(tickDate.getTime());
							Calendar createdCal = Calendar.getInstance();
							createdCal.setTimeInMillis(tickCreated.getTime());
							tickRepeatActivityTimestamp = LocalDateTime.of(tickCal.get(Calendar.YEAR), tickCal.get(Calendar.MONTH)+1, tickCal.get(Calendar.DAY_OF_MONTH), createdCal.get(Calendar.HOUR_OF_DAY), createdCal.get(Calendar.MINUTE), createdCal.get(Calendar.SECOND));
						}
						else if (tickDate != null) {
							Calendar tickCal = Calendar.getInstance();
							tickCal.setTimeInMillis(tickDate.getTime());
							tickRepeatActivityTimestamp = LocalDateTime.of(tickCal.get(Calendar.YEAR), tickCal.get(Calendar.MONTH)+1, tickCal.get(Calendar.DAY_OF_MONTH), 0, 0, 0);
						}
						psAddActivity.setTimestamp(1, tickRepeatActivityTimestamp == null? new Timestamp(0) : Timestamp.valueOf(tickRepeatActivityTimestamp));
						psAddActivity.setString(2, ACTIVITY_TYPE_TICK_REPEAT);
						psAddActivity.setInt(3, idProblem);
						psAddActivity.setNull(4, Types.INTEGER);
						psAddActivity.setInt(5, userId);
						psAddActivity.setNull(6, Types.INTEGER);
						psAddActivity.setInt(7, id);
						psAddActivity.addBatch();
					}
				}
			}

			/**
			 * Guestbook
			 */
			try (PreparedStatement ps = c.getConnection().prepareStatement("SELECT id, post_time FROM guestbook WHERE problem_id=? ORDER BY post_time")) {
				ps.setInt(1, idProblem);
				try (ResultSet rst = ps.executeQuery()) {
					while (rst.next()) {
						int id = rst.getInt("id");
						Timestamp postTime = rst.getTimestamp("post_time");
						psAddActivity.setTimestamp(1, postTime);
						psAddActivity.setString(2, ACTIVITY_TYPE_GUESTBOOK);
						psAddActivity.setInt(3, idProblem);
						psAddActivity.setNull(4, Types.INTEGER);
						psAddActivity.setNull(5, Types.INTEGER);
						psAddActivity.setInt(6, id);
						psAddActivity.setNull(7, Types.INTEGER);
						psAddActivity.addBatch();
					}
				}
			}

			/**
			 * Execute psAddActivity
			 */
			psAddActivity.executeBatch();
		}
	}

	public List<Activity> getActivity(int authUserId, Setup setup, int idArea, int idSector, int lowerGrade, boolean fa, boolean comments, boolean ticks, boolean media) throws SQLException {
		// GROUP_CONCAT has a max length 1024 characters by default, use this to ensure acitivity don't fail
		try (PreparedStatement ps = c.getConnection().prepareStatement("SET SESSION group_concat_max_len = 1000000")) {
			ps.execute();
		}

		Stopwatch stopwatch = Stopwatch.createStarted();
		final List<Activity> res = new ArrayList<>();
		/**
		 * Fetch activities to return
		 */
		final Set<Integer> faActivitityIds = new HashSet<>();
		final Set<Integer> tickActivitityIds = new HashSet<>();
		final Set<Integer> tickRepeatActivitityIds = new HashSet<>();
		final Set<Integer> mediaActivitityIds = new HashSet<>();
		final Set<Integer> guestbookActivitityIds = new HashSet<>();
		try (PreparedStatement ps = c.getConnection().prepareStatement("SELECT x.activity_timestamp, x.problem_id, p.locked_admin problem_locked_admin, p.locked_superadmin problem_locked_superadmin, p.name problem_name, t.subtype problem_subtype, p.grade, GROUP_CONCAT(concat(x.id,'-',x.type) SEPARATOR ',') activities" + 
				" FROM ((((((activity x INNER JOIN problem p ON x.problem_id=p.id) INNER JOIN type t ON p.type_id=t.id) INNER JOIN sector s ON p.sector_id=s.id) INNER JOIN area a ON s.area_id=a.id) INNER JOIN region r ON a.region_id=r.id) INNER JOIN region_type rt ON r.id=rt.region_id) LEFT JOIN user_region ur ON (r.id=ur.region_id AND ur.user_id=?)" + 
				" WHERE rt.type_id IN (SELECT type_id FROM region_type WHERE region_id=?) " +
				"   AND (r.id=? OR ur.user_id IS NOT NULL)" + 
				"   AND is_readable(ur.admin_read, ur.superadmin_read, a.locked_admin, a.locked_superadmin, a.trash)=1 AND is_readable(ur.admin_read, ur.superadmin_read, s.locked_admin, s.locked_superadmin, s.trash)=1 AND is_readable(ur.admin_read, ur.superadmin_read, p.locked_admin, p.locked_superadmin, p.trash)=1" + 
				(lowerGrade == 0? "" : " AND p.grade>=" + lowerGrade) +
				(fa? "" : " AND x.type!='FA'") +
				(comments? "" : " AND x.type!='GUESTBOOK'") +
				(ticks? "" : " AND x.type!='TICK' AND x.type!='TICK_REPEAT'") +
				(media? "" : " AND x.type!='MEDIA'") +
				(idArea==0? "" : " AND a.id=" + idArea) +
				(idSector==0? "" : " AND s.id=" + idSector) +
				" GROUP BY x.activity_timestamp, x.problem_id, p.locked_admin, p.locked_superadmin, p.name, p.grade" +
				" ORDER BY -x.activity_timestamp, x.problem_id DESC LIMIT 100")) {
			ps.setInt(1, authUserId);
			ps.setInt(2, setup.getIdRegion());
			ps.setInt(3, setup.getIdRegion());
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					Timestamp activityTimestamp = rst.getTimestamp("activity_timestamp");
					int problemId = rst.getInt("problem_id");
					boolean problemLockedAdmin = rst.getBoolean("problem_locked_admin");
					boolean problemLockedSuperadmin = rst.getBoolean("problem_locked_superadmin");
					String problemName = rst.getString("problem_name");
					String problemSubtype = rst.getString("problem_subtype");
					String grade = setup.getGradeConverter().getGradeFromIdGrade(rst.getInt("grade"));
					Set<Integer> activityIds = new HashSet<>();
					String activities = rst.getString("activities");
					for (String activity : activities.split(",")) {
						String[] str = activity.split("-");
						int idActivity = Integer.parseInt(str[0]);
						String type = str[1];
						activityIds.add(idActivity);
						switch (type) {
						case ACTIVITY_TYPE_FA: faActivitityIds.add(idActivity); break;
						case ACTIVITY_TYPE_TICK: tickActivitityIds.add(idActivity); break;
						case ACTIVITY_TYPE_TICK_REPEAT: tickRepeatActivitityIds.add(idActivity); break;
						case ACTIVITY_TYPE_GUESTBOOK: guestbookActivitityIds.add(idActivity); break;
						case ACTIVITY_TYPE_MEDIA: mediaActivitityIds.add(idActivity); break;
						default: throw new RuntimeException("Invalid type: " + type + " on idActivity=" + idActivity + " (acitivities=" + activities + ")");
						}
					}

					String timeAgo = TimeAgo.getTimeAgo(activityTimestamp.toLocalDateTime().toLocalDate());
					res.add(new Activity(activityIds, timeAgo, problemId, problemLockedAdmin, problemLockedSuperadmin, problemName, problemSubtype, grade));
				}
			}
		}

		if (!tickActivitityIds.isEmpty()) {
			try (PreparedStatement ps = c.getConnection().prepareStatement("SELECT a.id, u.id user_id, TRIM(CONCAT(u.firstname, ' ', COALESCE(u.lastname,''))) name, CASE WHEN u.picture IS NOT NULL THEN CONCAT('https://buldreinfo.com/buldreinfo_media/users/', u.id, '.jpg') END picture, t.comment description, t.stars, t.grade" + 
					" FROM activity a, tick t, user u" + 
					" WHERE a.id IN (" + Joiner.on(",").join(tickActivitityIds) + ")" + 
					"   AND a.user_id=u.id AND a.problem_id=t.problem_id AND u.id=t.user_id")) {
				try (ResultSet rst = ps.executeQuery()) {
					while (rst.next()) {
						int id = rst.getInt("id");
						Activity a = res.stream().filter(x -> x.getActivityIds().contains(id)).findAny().get();
						int userId = rst.getInt("user_id");
						String name = rst.getString("name");
						String picture = rst.getString("picture");
						String description = rst.getString("description");
						int stars = rst.getInt("stars");
						String personalGrade = setup.getGradeConverter().getGradeFromIdGrade(rst.getInt("grade"));
						a.setTick(false, userId, name, picture, description, stars, personalGrade);
					}
				}
			}
		}

		if (!tickRepeatActivitityIds.isEmpty()) {
			try (PreparedStatement ps = c.getConnection().prepareStatement("SELECT a.id, u.id user_id, TRIM(CONCAT(u.firstname, ' ', COALESCE(u.lastname,''))) name, CASE WHEN u.picture IS NOT NULL THEN CONCAT('https://buldreinfo.com/buldreinfo_media/users/', u.id, '.jpg') END picture, r.comment description, t.stars, t.grade" + 
					" FROM activity a, tick t, tick_repeat r, user u" + 
					" WHERE a.id IN (" + Joiner.on(",").join(tickRepeatActivitityIds) + ")" + 
					"   AND a.user_id=u.id AND a.problem_id=t.problem_id AND a.tick_repeat_id=r.id AND t.id=r.tick_id AND u.id=t.user_id")) {
				try (ResultSet rst = ps.executeQuery()) {
					while (rst.next()) {
						int id = rst.getInt("id");
						Activity a = res.stream().filter(x -> x.getActivityIds().contains(id)).findAny().get();
						int userId = rst.getInt("user_id");
						String name = rst.getString("name");
						String picture = rst.getString("picture");
						String description = rst.getString("description");
						int stars = rst.getInt("stars");
						String personalGrade = setup.getGradeConverter().getGradeFromIdGrade(rst.getInt("grade"));
						a.setTick(true, userId, name, picture, description, stars, personalGrade);
					}
				}
			}
		}

		if (!guestbookActivitityIds.isEmpty()) {
			try (PreparedStatement ps = c.getConnection().prepareStatement("SELECT a.id, u.id user_id, TRIM(CONCAT(u.firstname, ' ', COALESCE(u.lastname,''))) name, CASE WHEN u.picture IS NOT NULL THEN CONCAT('https://buldreinfo.com/buldreinfo_media/users/', u.id, '.jpg') END picture, g.message, mg.media_id, m.checksum" + 
					" FROM (((activity a INNER JOIN guestbook g ON a.guestbook_id=g.id) INNER JOIN user u ON g.user_id=u.id) LEFT JOIN media_guestbook mg ON g.id=mg.guestbook_id) LEFT JOIN media m ON (mg.media_id=m.id AND m.deleted_user_id IS NULL AND m.is_movie=0)" + 
					" WHERE a.id IN (" + Joiner.on(",").join(guestbookActivitityIds) + ")")) {
				try (ResultSet rst = ps.executeQuery()) {
					while (rst.next()) {
						int id = rst.getInt("id");
						Activity a = res.stream().filter(x -> x.getActivityIds().contains(id)).findAny().get();
						int userId = rst.getInt("user_id");
						String name = rst.getString("name");
						String picture = rst.getString("picture");
						String message = rst.getString("message");
						a.setGuestbook(userId, name, picture, message);

						int mediaId = rst.getInt("media_id");
						int crc32 = rst.getInt("checksum");
						if (mediaId > 0) {
							boolean isMovie = false;
							String embedUrl = null;
							a.addMedia(mediaId, crc32, isMovie, embedUrl);
						}
					}
				}
			}
		}

		if (!faActivitityIds.isEmpty()) {
			try (PreparedStatement ps = c.getConnection().prepareStatement("SELECT a.id, TRIM(CONCAT(u.firstname, ' ', COALESCE(u.lastname,''))) name, u.id user_id, CASE WHEN u.picture IS NOT NULL THEN CONCAT('https://buldreinfo.com/buldreinfo_media/users/', u.id, '.jpg') END picture, p.description, MAX(m.id) random_media_id, MAX(m.checksum) random_media_crc32" + 
					" FROM ((((activity a INNER JOIN problem p ON a.problem_id=p.id) LEFT JOIN fa ON p.id=fa.problem_id) LEFT JOIN user u ON fa.user_id=u.id) LEFT JOIN media_problem mp ON p.id=mp.problem_id) LEFT JOIN media m ON (mp.media_id=m.id AND m.deleted_user_id IS NULL AND m.is_movie=0)" + 
					" WHERE a.id IN (" + Joiner.on(",").join(faActivitityIds) + ")" + 
					" GROUP BY a.id, u.firstname, u.lastname, u.id, u.picture, p.description" +
					" ORDER BY u.firstname, u.lastname")) {
				try (ResultSet rst = ps.executeQuery()) {
					while (rst.next()) {
						int id = rst.getInt("id");
						Activity a = res.stream().filter(x -> x.getActivityIds().contains(id)).findAny().get();
						String name = rst.getString("name");
						int userId = rst.getInt("user_id");
						String picture = rst.getString("picture");
						String description = rst.getString("description");
						int problemRandomMediaId = rst.getInt("random_media_id");
						int problemRandomMediaCrc32 = rst.getInt("random_media_crc32");
						a.addFa(name, userId, picture, description, problemRandomMediaId, problemRandomMediaCrc32);
					}
				}
			}
		}

		if (!mediaActivitityIds.isEmpty()) {
			try (PreparedStatement ps = c.getConnection().prepareStatement("SELECT a.id, m.id media_id, m.checksum media_crc32, m.is_movie, m.embed_url" + 
					" FROM activity a, media m, media_problem mp" + 
					" WHERE a.id IN (" + Joiner.on(",").join(mediaActivitityIds) + ")" + 
					"   AND a.media_id=m.id AND m.id=mp.media_id AND a.problem_id=mp.problem_id" +
					" ORDER BY mp.sorting, m.id")) {
				try (ResultSet rst = ps.executeQuery()) {
					while (rst.next()) {
						int id = rst.getInt("id");
						Activity a = res.stream().filter(x -> x.getActivityIds().contains(id)).findAny().get();
						int mediaId = rst.getInt("media_id");
						int mediaCrc32 = rst.getInt("media_crc32");
						boolean isMovie = rst.getBoolean("is_movie");
						String embedUrl = rst.getString("embed_url");
						a.addMedia(mediaId, mediaCrc32, isMovie, embedUrl);
					}
				}
			}
		}
		logger.debug("getActivity(authUserId={}, setup={}) - res.size()={}, duration={}", authUserId, setup, res.size(), stopwatch);
		return res;
	}

	public List<Administrator> getAdministrators(int idRegion) throws SQLException {
		List<Administrator> res = new ArrayList<>();
		try (PreparedStatement ps = c.getConnection().prepareStatement("SELECT u.id, TRIM(CONCAT(u.firstname, ' ', COALESCE(u.lastname,''))) name, CASE WHEN u.picture IS NOT NULL THEN CONCAT('https://buldreinfo.com/buldreinfo_media/users/', u.id, '.jpg') ELSE '' END picture, DATE_FORMAT(MAX(l.when),'%Y.%m.%d') last_login FROM (user u INNER JOIN user_login l ON u.id=l.user_id) LEFT JOIN user_region ur ON (u.id=ur.user_id AND l.region_id=ur.region_id) WHERE l.region_id=? AND (ur.admin_write=1 OR ur.superadmin_write=1) GROUP BY u.id, u.firstname, u.lastname, u.picture ORDER BY TRIM(CONCAT(u.firstname, ' ', COALESCE(u.lastname,'')))")) {
			ps.setInt(1, idRegion);
			try (ResultSet rst = ps.executeQuery()) {
				final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd");
				while (rst.next()) {
					int userId = rst.getInt("id");
					String name = rst.getString("name");
					String picture = rst.getString("picture");
					String lastLogin = rst.getString("last_login");
					String timeAgo = TimeAgo.getTimeAgo(LocalDate.parse(lastLogin, formatter));
					res.add(new Administrator(userId, name, picture, timeAgo));
				}
			}
		}
		return res;
	}

	public Area getArea(Setup s, int authUserId, int reqId) throws IOException, SQLException {
		Stopwatch stopwatch = Stopwatch.createStarted();
		try (PreparedStatement ps = c.getConnection().prepareStatement("UPDATE area SET hits=hits+1 WHERE id=?")) {
			ps.setInt(1, reqId);
			ps.execute();
		}
		Area a = null;
		try (PreparedStatement ps = c.getConnection().prepareStatement("SELECT r.id region_id, CONCAT(r.url,'/area/',a.id) canonical, a.locked_admin, a.locked_superadmin, a.for_developers, a.access_info, a.access_closed, a.no_dogs_allowed, a.sun_from_hour, a.sun_to_hour, a.name, a.description, c.id coordinates_id, c.latitude, c.longitude, c.elevation, c.elevation_source, a.hits FROM ((area a INNER JOIN region r ON a.region_id=r.id) LEFT JOIN coordinates c ON a.coordinates_id=c.id) LEFT JOIN user_region ur ON a.region_id=ur.region_id AND ur.user_id=? WHERE a.id=? AND (r.id=? OR ur.user_id IS NOT NULL) AND is_readable(ur.admin_read, ur.superadmin_read, a.locked_admin, a.locked_superadmin, a.trash)=1 GROUP BY r.id, r.url, a.locked_admin, a.locked_superadmin, a.for_developers, a.access_info, a.access_closed, a.no_dogs_allowed, a.name, a.sun_from_hour, a.sun_to_hour, a.description, c.id, c.latitude, c.longitude, c.elevation, c.elevation_source, a.hits")) {
			ps.setInt(1, authUserId);
			ps.setInt(2, reqId);
			ps.setInt(3, s.getIdRegion());
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					int regionId = rst.getInt("region_id");
					String canonical = rst.getString("canonical");
					boolean lockedAdmin = rst.getBoolean("locked_admin");
					boolean lockedSuperadmin = rst.getBoolean("locked_superadmin");
					boolean forDevelopers = rst.getBoolean("for_developers");
					String accessInfo = rst.getString("access_info");
					String accessClosed = rst.getString("access_closed");
					boolean noDogsAllowed = rst.getBoolean("no_dogs_allowed");
					int sunFromHour = rst.getInt("sun_from_hour");
					int sunToHour = rst.getInt("sun_to_hour");
					String name = rst.getString("name");
					String comment = rst.getString("description");
					int idCoordinates = rst.getInt("coordinates_id");
					Coordinates coordinates = idCoordinates == 0? null : new Coordinates(idCoordinates, rst.getDouble("latitude"), rst.getDouble("longitude"), rst.getDouble("elevation"), rst.getString("elevation_source"));
					int hits = rst.getInt("hits");
					List<Media> media = null;
					List<Media> triviaMedia = null;
					List<Media> allMedia = getMediaArea(authUserId, reqId, false);
					if (allMedia != null && allMedia.size() > 0) {
						media = allMedia.stream().filter(x -> !x.isTrivia()).collect(Collectors.toList());
						if (media.size() != allMedia.size()) {
							triviaMedia = allMedia.stream().filter(x -> x.isTrivia()).collect(Collectors.toList());
						}
					}
					a = new Area(null, regionId, canonical, reqId, false, lockedAdmin, lockedSuperadmin, forDevelopers, accessInfo, accessClosed, noDogsAllowed, sunFromHour, sunToHour, name, comment, coordinates, -1, -1, media, triviaMedia, null, hits);
				}
			}
		}
		if (a == null) {
			// Area not found, see if it's visible on a different domain
			Redirect res = c.getBuldreinfoRepo().getCanonicalUrl(reqId, 0, 0);
			if (!Strings.isNullOrEmpty(res.getRedirectUrl())) {
				return new Area(res.getRedirectUrl(), -1, null, -1, false, false, false, false, null, null, false, 0, 0, null, null, null, 0, 0, null, null, null, 0);
			}
		}
		Preconditions.checkNotNull(a, "Could not find area with id=" + reqId);
		Map<Integer, Area.AreaSector> sectorLookup = new HashMap<>();
		try (PreparedStatement ps = c.getConnection().prepareStatement("SELECT s.id, s.sorting, s.locked_admin, s.locked_superadmin, s.name, s.description, s.access_info, s.access_closed, c.id coordinates_id, c.latitude, c.longitude, c.elevation, c.elevation_source, s.compass_direction_id_calculated, s.compass_direction_id_manual, MAX(m.id) media_id, MAX(m.checksum) media_crc32 FROM (((((area a INNER JOIN sector s ON a.id=s.area_id) LEFT JOIN coordinates c ON s.parking_coordinates_id=c.id) LEFT JOIN user_region ur ON a.region_id=ur.region_id AND ur.user_id=?) LEFT JOIN problem p ON s.id=p.sector_id AND is_readable(ur.admin_read, ur.superadmin_read, p.locked_admin, p.locked_superadmin, p.trash)=1) LEFT JOIN media_problem mp ON p.id=mp.problem_id) LEFT JOIN media m ON mp.media_id=m.id AND m.is_movie=0 AND m.deleted_user_id IS NULL WHERE a.id=? AND is_readable(ur.admin_read, ur.superadmin_read, s.locked_admin, s.locked_superadmin, s.trash)=1 GROUP BY s.id, s.sorting, s.locked_admin, s.locked_superadmin, s.name, s.description, s.access_info, c.id, c.latitude, c.longitude, c.elevation, c.elevation_source, s.compass_direction_id_calculated, s.compass_direction_id_manual ORDER BY s.sorting, s.name")) {
			ps.setInt(1, authUserId);
			ps.setInt(2, reqId);
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					int id = rst.getInt("id");
					int sorting = rst.getInt("sorting");
					boolean lockedAdmin = rst.getBoolean("locked_admin");
					boolean lockedSuperadmin = rst.getBoolean("locked_superadmin");
					String name = rst.getString("name");
					String comment = rst.getString("description");
					String accessInfo = rst.getString("access_info");
					String accessClosed = rst.getString("access_closed");
					int idCoordinates = rst.getInt("coordinates_id");
					Coordinates parking = idCoordinates == 0? null : new Coordinates(idCoordinates, rst.getDouble("latitude"), rst.getDouble("longitude"), rst.getDouble("elevation"), rst.getString("elevation_source"));
					CompassDirection wallDirectionCalculated = getCompassDirection(s, rst.getInt("compass_direction_id_calculated"));
					CompassDirection wallDirectionManual = getCompassDirection(s, rst.getInt("compass_direction_id_manual"));
					int randomMediaId = rst.getInt("media_id");
					int randomMediaCrc32 = rst.getInt("media_crc32");
					if (randomMediaId == 0) {
						boolean inherited = false;
						boolean showHiddenMedia = true; // Show everything to ensure image in area overview
						List<Media> x = getMediaSector(s, authUserId, id, 0, inherited, 0, 0, showHiddenMedia);
						if (!x.isEmpty()) {
							randomMediaId = x.get(0).getId();
						}
					}
					Area.AreaSector as = a.addSector(id, sorting, lockedAdmin, lockedSuperadmin, name, comment, accessInfo, accessClosed, parking, wallDirectionCalculated, wallDirectionManual, randomMediaId, randomMediaCrc32);
					sectorLookup.put(id, as);
					for (SectorProblem sp : getSectorProblems(s, authUserId, as.getId())) {
						as.getProblems().add(sp);
					}
				}
			}
		}
		if (!sectorLookup.isEmpty()) {
			// Fill sector outlines
			Multimap<Integer, Coordinates> idSectorOutline = getSectorOutlines(sectorLookup.keySet());
			for (int idSector : idSectorOutline.keySet()) {
				List<Coordinates> outline = Lists.newArrayList(idSectorOutline.get(idSector));
				sectorLookup.get(idSector).setOutline(outline);
			}
			// Fill sector approaches
			Map<Integer, Approach> idSectorApproach = getSectorApproaches(sectorLookup.keySet());
			for (int idSector : idSectorApproach.keySet()) {
				sectorLookup.get(idSector).setApproach(idSectorApproach.get(idSector));
			}
		}
		try (PreparedStatement ps = c.getConnection().prepareStatement("SELECT s.id, CASE WHEN p.grade IS NULL OR p.grade=0 THEN 'Projects' WHEN p.broken IS NOT NULL THEN 'Broken' ELSE CONCAT(ty.type, 's', CASE WHEN ty.subtype IS NOT NULL THEN CONCAT(' (',ty.subtype,')') ELSE '' END) END type, COUNT(DISTINCT p.id) num, COUNT(DISTINCT CASE WHEN f.problem_id IS NOT NULL OR t.id IS NOT NULL THEN p.id END) num_ticked FROM (((((area a INNER JOIN sector s ON a.id=s.area_id) INNER JOIN problem p ON s.id=p.sector_id) INNER JOIN type ty ON p.type_id=ty.id) LEFT JOIN fa f ON p.id=f.problem_id AND f.user_id=?) LEFT JOIN tick t ON p.id=t.problem_id AND t.user_id=?) LEFT JOIN user_region ur ON a.region_id=ur.region_id AND ur.user_id=? WHERE a.id=? AND is_readable(ur.admin_read, ur.superadmin_read, s.locked_admin, s.locked_superadmin, s.trash)=1 AND is_readable(ur.admin_read, ur.superadmin_read, p.locked_admin, p.locked_superadmin, p.trash)=1 GROUP BY s.id, CASE WHEN p.grade IS NULL OR p.grade=0 THEN 'Projects' WHEN p.broken IS NOT NULL THEN 'Broken' ELSE CONCAT(ty.type, 's', CASE WHEN ty.subtype IS NOT NULL THEN CONCAT(' (',ty.subtype,')') ELSE '' END) END ORDER BY s.id, CASE WHEN p.grade IS NULL OR p.grade=0 THEN 'Projects' WHEN p.broken IS NOT NULL THEN 'Broken' ELSE CONCAT(ty.type, 's', CASE WHEN ty.subtype IS NOT NULL THEN CONCAT(' (',ty.subtype,')') ELSE '' END) END")) {
			ps.setInt(1, authUserId);
			ps.setInt(2, authUserId);
			ps.setInt(3, authUserId);
			ps.setInt(4, reqId);
			Map<String, TypeNumTicked> lookup = new LinkedHashMap<>();
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					int sectorId = rst.getInt("id");
					String type = rst.getString("type");
					int num = rst.getInt("num");
					int numTicked = rst.getInt("num_ticked");
					TypeNumTicked typeNumTicked = new TypeNumTicked(type, num, numTicked);
					// Sector
					Optional<Area.AreaSector> optSector = a.getSectors().stream().filter(x -> x.getId() == sectorId).findAny();
					if (optSector.isPresent()) {
						optSector.get().getTypeNumTicked().add(typeNumTicked);
					}
					// Area
					TypeNumTicked areaTnt = lookup.get(type);
					if (areaTnt == null) {
						areaTnt = new TypeNumTicked(type, num, numTicked);
						a.getTypeNumTicked().add(areaTnt);
						lookup.put(type, areaTnt);
					}
					else {
						areaTnt.addNum(num);
						areaTnt.addTicked(numTicked);
					}
				}
			}
		}
		a.orderSectors();
		logger.debug("getArea(authUserId={}, reqId={}) - duration={}", authUserId, reqId, stopwatch);
		return a;
	}
	
	public Collection<Area> getAreaList(int authUserId, int reqIdRegion) throws IOException, SQLException {
		Stopwatch stopwatch = Stopwatch.createStarted();
		List<Area> res = new ArrayList<>();
		try (PreparedStatement ps = c.getConnection().prepareStatement("SELECT r.id region_id, CONCAT(r.url,'/area/',a.id) canonical, a.id, a.locked_admin, a.locked_superadmin, a.for_developers, a.access_info, a.access_closed, a.no_dogs_allowed, a.sun_from_hour, a.sun_to_hour, a.name, a.description, c.id coordinates_id, c.latitude, c.longitude, c.elevation, c.elevation_source, COUNT(DISTINCT s.id) num_sectors, COUNT(DISTINCT p.id) num_problems, a.hits FROM (((((area a INNER JOIN region r ON a.region_id=r.id) INNER JOIN region_type rt ON r.id=rt.region_id) LEFT JOIN coordinates c ON a.coordinates_id=c.id) LEFT JOIN sector s ON a.id=s.area_id) LEFT JOIN problem p ON s.id=p.sector_id) LEFT JOIN user_region ur ON (r.id=ur.region_id AND ur.user_id=?) WHERE rt.type_id IN (SELECT type_id FROM region_type WHERE region_id=?) AND (a.region_id=? OR ur.user_id IS NOT NULL) AND is_readable(ur.admin_read, ur.superadmin_read, a.locked_admin, a.locked_superadmin, a.trash)=1 GROUP BY r.id, r.url, a.id, a.locked_admin, a.locked_superadmin, a.for_developers, a.access_info, a.access_closed, a.no_dogs_allowed, a.sun_from_hour, a.sun_to_hour, a.name, a.description, c.id, c.latitude, c.longitude, c.elevation, c.elevation_source, a.hits ORDER BY replace(replace(replace(lower(a.name),'�','zx'),'�','zy'),'�','zz')")) {
			ps.setInt(1, authUserId);
			ps.setInt(2, reqIdRegion);
			ps.setInt(3, reqIdRegion);
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					int idRegion = rst.getInt("region_id");
					String canonical = rst.getString("canonical");
					int id = rst.getInt("id");
					boolean lockedAdmin = rst.getBoolean("locked_admin");
					boolean lockedSuperadmin = rst.getBoolean("locked_superadmin");
					boolean forDevelopers = rst.getBoolean("for_developers");
					String accessInfo = rst.getString("access_info");
					String accessClosed = rst.getString("access_closed");
					boolean noDogsAllowed = rst.getBoolean("no_dogs_allowed");
					int sunFromHour = rst.getInt("sun_from_hour");
					int sunToHour = rst.getInt("sun_to_hour");
					String name = rst.getString("name");
					String comment = rst.getString("description");
					if (comment != null) {
						int ix = comment.indexOf("<strong>Forhold:</strong>");
						if (ix != -1) {
							comment = comment.substring(ix+25);
							ix = comment.indexOf("<strong>");
							comment = comment.substring(0, ix);
						}
					}
					int idCoordinates = rst.getInt("coordinates_id");
					Coordinates coordinates = idCoordinates == 0? null : new Coordinates(idCoordinates, rst.getDouble("latitude"), rst.getDouble("longitude"), rst.getDouble("elevation"), rst.getString("elevation_source"));
					int numSectors = rst.getInt("num_sectors");
					int numProblems = rst.getInt("num_problems");
					int hits = rst.getInt("hits");
					res.add(new Area(null, idRegion, canonical, id, false, lockedAdmin, lockedSuperadmin, forDevelopers, accessInfo, accessClosed, noDogsAllowed, sunFromHour, sunToHour, name, comment, coordinates, numSectors, numProblems, null, null, null, hits));
				}
			}
		}
		logger.debug("getAreaList(authUserId={}, reqIdRegion={}) - res.size()={} - duration={}", authUserId, reqIdRegion, res.size(), stopwatch);
		return res;
	}

	public int getAuthUserId(Auth0Profile profile) throws SQLException, NoSuchAlgorithmException, IOException {
		int authUserId = -1;
		String picture = null;
		try (PreparedStatement ps = c.getConnection().prepareStatement("SELECT e.user_id, u.picture FROM user_email e, user u WHERE e.user_id=u.id AND lower(e.email)=?")) {
			ps.setString(1, profile.getEmail());
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					authUserId = rst.getInt("user_id");
					picture = rst.getString("picture");
				}
			}
		}
		if (authUserId == -1 && profile.getName() != null) {
			try (PreparedStatement ps = c.getConnection().prepareStatement("SELECT id, picture FROM user WHERE TRIM(CONCAT(firstname, ' ', COALESCE(lastname,'')))=?")) {
				ps.setString(1, profile.getName());
				try (ResultSet rst = ps.executeQuery()) {
					while (rst.next()) {
						authUserId = rst.getInt("id");
						picture = rst.getString("picture");
						// Add email to user
						try (PreparedStatement ps2 = c.getConnection().prepareStatement("INSERT INTO user_email (user_id, email) VALUES (?, ?)")) {
							ps2.setInt(1, authUserId);
							ps2.setString(2, profile.getEmail());
							ps2.execute();
						}
					}
				}
			}
		}
		if (authUserId == -1) {
			final boolean autoCommit = true;
			authUserId = addUser(profile.getEmail(), profile.getFirstname(), profile.getLastname(), profile.getPicture(), autoCommit);
		}
		else if (profile.getPicture() != null && (picture == null || !picture.equals(profile.getPicture()))) {
			if (picture != null && picture.contains("fbsbx.com") && !profile.getPicture().contains("fbsbx.com")) {
				logger.debug("Dont change from facebook-image, new image is most likely avatar with text...");
			} else {
				if (GlobalFunctions.downloadUserImage(authUserId, profile.getPicture())) {
					try (PreparedStatement ps = c.getConnection().prepareStatement("UPDATE user SET picture=? WHERE id=?")) {
						ps.setString(1, profile.getPicture());
						ps.setInt(2, authUserId);
						ps.executeUpdate();
					}
				}
			}
		}
		logger.debug("getAuthUserId(profile={}) - authUserId={}", profile, authUserId);
		return authUserId;
	}

	public Redirect getCanonicalUrl(int idArea, int idSector, int idProblem) throws SQLException {
		String sqlStr = null;
		int id = 0;
		if (idArea > 0) {
			sqlStr = "SELECT CONCAT(r.url,'/area/',a.id) url FROM region r, area a WHERE r.id=a.region_id AND a.locked_admin=0 AND a.locked_superadmin=0 AND a.id=?";
			id = idArea;
		}
		else if (idSector > 0) {
			sqlStr = "SELECT CONCAT(r.url,'/sector/',s.id) url FROM region r, area a, sector s WHERE r.id=a.region_id AND a.id=s.area_id AND a.locked_admin=0 AND a.locked_superadmin=0 AND s.locked_admin=0 AND s.locked_superadmin=0 AND s.id=?";
			id = idSector;
		}
		else if (idProblem > 0) {
			sqlStr = "SELECT CONCAT(r.url,'/problem/',p.id) url FROM region r, area a, sector s, problem p WHERE r.id=a.region_id AND a.id=s.area_id AND s.id=p.sector_id AND a.locked_admin=0 AND a.locked_superadmin=0 AND s.locked_admin=0 AND s.locked_superadmin=0 AND p.locked_admin=0 AND p.locked_superadmin=0 AND p.id=?";
			id = idProblem;
		}
		Preconditions.checkArgument(id > 0 && sqlStr != null, "Invalid parameters: idArea=" + idArea + ", idSector=" + idSector + ", idProblem=" + idProblem);
		Redirect res = null;
		try (PreparedStatement ps = c.getConnection().prepareStatement(sqlStr)) {
			ps.setInt(1, id);
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					res = new Redirect(rst.getString("url"), 0, 0, 0);
				}
			}
		}
		Preconditions.checkNotNull(res, "Could not find canonical url for idArea=" + idArea + ", idSector=" + idSector + ", idProblem=" + idProblem);
		return res;
	}

	public Collection<GradeDistribution> getContentGraph(int authUserId, Setup setup) throws SQLException {
		Map<String, GradeDistribution> res = new LinkedHashMap<>();
		String sqlStr = "WITH x AS ("
				+ " SELECT g.base_no grade_base_no, x.region, x.t, COUNT(id_problem) num"
				+ " FROM (SELECT r.name region, s.sorting, ty.subtype t, ROUND((IFNULL(SUM(nullif(t.grade,-1)),0) + p.grade) / (COUNT(t.grade) + 1)) grade_id, p.id id_problem"
				+ "   FROM ((((((region r INNER JOIN region_type rt ON r.id=rt.region_id) INNER JOIN area a ON r.id=a.region_id) INNER JOIN sector s ON a.id=s.area_id) INNER JOIN problem p ON s.id=p.sector_id) INNER JOIN type ty ON p.type_id=ty.id) LEFT JOIN user_region ur ON r.id=ur.region_id AND ur.user_id=?) LEFT JOIN tick t ON (p.id=t.problem_id AND t.grade>0)"
				+ "   WHERE rt.type_id IN (SELECT type_id FROM region_type WHERE region_id=?)"
				+ "     AND (a.region_id=? OR ur.user_id IS NOT NULL)"
				+ "     AND is_readable(ur.admin_read, ur.superadmin_read, p.locked_admin, p.locked_superadmin, p.trash)=1"
				+ "   GROUP BY s.name, ty.subtype, p.id) x, grade g"
				+ " WHERE x.grade_id=g.grade_id AND g.t=?"
				+ " GROUP BY x.region, g.base_no, x.t"
				+ " )"
				+ " SELECT g.base_no grade, x.region, COALESCE(x.t,'Boulder') t, num"
				+ " FROM (SELECT g.base_no, MIN(g.grade_id) sort FROM grade g WHERE g.t=? GROUP BY g.base_no) g LEFT JOIN x ON g.base_no=x.grade_base_no"
				+ " ORDER BY g.sort, x.region, x.t";
		try (PreparedStatement ps = c.getConnection().prepareStatement(sqlStr)) {
			ps.setInt(1, authUserId);
			ps.setInt(2, setup.getIdRegion());
			ps.setInt(3, setup.getIdRegion());
			ps.setString(4, setup.getGradeSystem().toString());
			ps.setString(5, setup.getGradeSystem().toString());
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					String grade = rst.getString("grade");
					GradeDistribution g = res.get(grade);
					if (g == null) {
						g = new GradeDistribution(grade);
						res.put(grade, g);
					}
					String region = rst.getString("region");
					if (region != null) {
						String t = rst.getString("t");
						int num = rst.getInt("num");
						g.addSector(region, t, num);
					}
				}
			}
		}
		return res.values();
	}

	public Collection<Dangerous> getDangerous(int authUserId, Setup setup) throws SQLException {
		Map<Integer, Dangerous> areasLookup = new LinkedHashMap<>();
		Map<Integer, Dangerous.DangerousSector> sectorLookup = new HashMap<>();
		try (PreparedStatement ps = c.getConnection().prepareStatement("SELECT a.id area_id, CONCAT(r.url,'/area/',a.id) area_url, a.name area_name, a.locked_admin area_locked_admin, a.locked_superadmin area_locked_superadmin, a.sun_from_hour area_sun_from_hour, a.sun_to_hour area_sun_to_hour, s.id sector_id, CONCAT(r.url,'/sector/',s.id) sector_url, s.name sector_name, s.compass_direction_id_calculated sector_compass_direction_id_calculated, s.compass_direction_id_manual sector_compass_direction_id_manual, s.locked_admin sector_locked_admin, s.locked_superadmin sector_locked_superadmin, p.id problem_id, CONCAT(r.url,'/problem/',p.id) problem_url, p.broken problem_broken, p.nr problem_nr, p.grade problem_grade, p.name problem_name, p.locked_admin problem_locked_admin, p.locked_superadmin problem_locked_superadmin, TRIM(CONCAT(u.firstname, ' ', COALESCE(u.lastname,''))) name, DATE_FORMAT(g.post_time,'%Y.%m.%d') post_time, g.message FROM ((((((area a INNER JOIN region r ON r.id=a.region_id) INNER JOIN region_type rt ON r.id=rt.region_id) INNER JOIN sector s ON a.id=s.area_id) INNER JOIN problem p ON s.id=p.sector_id) INNER JOIN guestbook g ON p.id=g.problem_id AND g.danger=1 AND g.id IN (SELECT MAX(id) id FROM guestbook WHERE danger=1 OR resolved=1 GROUP BY problem_id)) INNER JOIN user u ON g.user_id=u.id) LEFT JOIN user_region ur ON a.region_id=ur.region_id AND ur.user_id=? WHERE rt.type_id IN (SELECT type_id FROM region_type WHERE region_id=?) AND (a.region_id=? OR ur.user_id IS NOT NULL) AND is_readable(ur.admin_read, ur.superadmin_read, p.locked_admin, p.locked_superadmin, p.trash)=1 GROUP BY a.id, a.name, a.locked_admin, a.locked_superadmin, a.sun_from_hour, a.sun_to_hour, s.id, s.name, s.compass_direction_id_calculated, s.compass_direction_id_manual, s.locked_admin, s.locked_superadmin, p.id, p.broken, p.nr, p.grade, p.name, p.locked_admin, p.locked_superadmin, u.firstname, u.lastname, g.post_time, g.message ORDER BY a.name, s.name, p.nr")) {
			ps.setInt(1, authUserId);
			ps.setInt(2, setup.getIdRegion());
			ps.setInt(3, setup.getIdRegion());
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					// Area
					int areaId = rst.getInt("area_id");
					Dangerous a = areasLookup.get(areaId);
					if (a == null) {
						String areaUrl = rst.getString("area_url");
						String areaName = rst.getString("area_name");
						boolean areaLockedAdmin = rst.getBoolean("area_locked_admin"); 
						boolean areaLockedSuperadmin = rst.getBoolean("area_locked_superadmin");
						int areaSunFromHour = rst.getInt("area_sun_from_hour");
						int areaSunToHour = rst.getInt("area_sun_to_hour");
						a = new Dangerous(areaId, areaUrl, areaName, areaLockedAdmin, areaLockedSuperadmin, areaSunFromHour, areaSunToHour);
						areasLookup.put(areaId, a);
					}
					// Sector
					int sectorId = rst.getInt("sector_id");
					Dangerous.DangerousSector s = sectorLookup.get(sectorId);
					if (s == null) {
						String sectorUrl = rst.getString("sector_url");
						String sectorName = rst.getString("sector_name");
						CompassDirection sectorWallDirectionCalculated = getCompassDirection(setup, rst.getInt("sector_compass_direction_id_calculated"));
						CompassDirection sectorWallDirectionManual = getCompassDirection(setup, rst.getInt("sector_compass_direction_id_manual"));
						boolean sectorLockedAdmin = rst.getBoolean("sector_locked_admin"); 
						boolean sectorLockedSuperadmin = rst.getBoolean("sector_locked_superadmin");
						s = a.addSector(sectorId, sectorUrl, sectorName, sectorWallDirectionCalculated, sectorWallDirectionManual, sectorLockedAdmin, sectorLockedSuperadmin);
						sectorLookup.put(sectorId, s);
					}
					// Problem
					int id = rst.getInt("problem_id");
					String url = rst.getString("problem_url");
					String broken = rst.getString("problem_broken");
					int nr = rst.getInt("problem_nr");
					int grade = rst.getInt("problem_grade");
					boolean lockedAdmin = rst.getBoolean("problem_locked_admin"); 
					boolean lockedSuperadmin = rst.getBoolean("problem_locked_superadmin");
					String name = rst.getString("problem_name");
					String postBy = rst.getString("name");
					String postWhen = rst.getString("post_time");
					String postTxt = rst.getString("message");
					s.addProblem(id, url, broken, lockedAdmin, lockedSuperadmin, nr, name, setup.getGradeConverter().getGradeFromIdGrade(grade), postBy, postWhen, postTxt);
				}
			}
		}
		return areasLookup.values();
	}
	
	public Frontpage getFrontpage(int authUserId, Setup setup) throws SQLException {
		Stopwatch stopwatch = Stopwatch.createStarted();
		Frontpage res = new Frontpage();
		try (PreparedStatement ps = c.getConnection().prepareStatement("SELECT COUNT(DISTINCT p.id) num_problems, COUNT(DISTINCT CASE WHEN p.coordinates_id IS NOT NULL THEN p.id END) num_problems_with_coordinates, COUNT(DISTINCT svg.problem_id) num_problems_with_topo FROM (((((area a INNER JOIN region r ON a.region_id=r.id AND a.trash IS NULL) INNER JOIN region_type rt ON r.id=rt.region_id) INNER JOIN sector s ON a.id=s.area_id AND s.trash IS NULL) INNER JOIN problem p ON s.id=p.sector_id AND p.trash IS NULL) LEFT JOIN user_region ur ON (r.id=ur.region_id AND ur.user_id=?)) LEFT JOIN svg ON p.id=svg.problem_id WHERE rt.type_id IN (SELECT type_id FROM region_type WHERE region_id=?) AND (a.region_id=? OR ur.user_id IS NOT NULL)")) {
			ps.setInt(1, authUserId);
			ps.setInt(2, setup.getIdRegion());
			ps.setInt(3, setup.getIdRegion());
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					res.setNumProblems(rst.getInt("num_problems"));
					res.setNumProblemsWithCoordinates(rst.getInt("num_problems_with_coordinates"));
					res.setNumProblemsWithTopo(rst.getInt("num_problems_with_topo"));
				}
			}
		}
		try (PreparedStatement ps = c.getConnection().prepareStatement("SELECT COUNT(DISTINCT CASE WHEN m.is_movie=0 THEN mp.id END) num_images, COUNT(DISTINCT CASE WHEN m.is_movie=1 THEN mp.id END) num_movies FROM ((((((media m INNER JOIN media_problem mp ON m.id=mp.media_id) INNER JOIN problem p ON mp.problem_id=p.id) INNER JOIN sector s ON p.sector_id=s.id) INNER JOIN area a ON s.area_id=a.id) INNER JOIN region r ON a.region_id=r.id) INNER JOIN region_type rt ON r.id=rt.region_id) LEFT JOIN user_region ur ON (r.id=ur.region_id AND ur.user_id=?) WHERE rt.type_id IN (SELECT type_id FROM region_type WHERE region_id=?) AND a.trash IS NULL AND s.trash IS NULL AND p.trash IS NULL AND m.deleted_user_id IS NULL AND (a.region_id=? OR ur.user_id IS NOT NULL)")) {
			ps.setInt(1, authUserId);
			ps.setInt(2, setup.getIdRegion());
			ps.setInt(3, setup.getIdRegion());
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					res.setNumImages(rst.getInt("num_images"));
					res.setNumMovies(rst.getInt("num_movies"));
				}
			}
		}
		try (PreparedStatement ps = c.getConnection().prepareStatement("SELECT COUNT(DISTINCT t.id) num_ticks FROM (((((tick t INNER JOIN problem p ON t.problem_id=p.id AND p.trash IS NULL) INNER JOIN sector s ON p.sector_id=s.id AND s.trash IS NULL) INNER JOIN area a ON s.area_id=a.id AND a.trash IS NULL) INNER JOIN region r ON a.region_id=r.id) INNER JOIN region_type rt ON r.id=rt.region_id) LEFT JOIN user_region ur ON (r.id=ur.region_id AND ur.user_id=?) WHERE rt.type_id IN (SELECT type_id FROM region_type WHERE region_id=?) AND (a.region_id=? OR ur.user_id IS NOT NULL)")) {
			ps.setInt(1, authUserId);
			ps.setInt(2, setup.getIdRegion());
			ps.setInt(3, setup.getIdRegion());
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					res.setNumTicks(rst.getInt("num_ticks"));
				}
			}
		}

		/**
		 * RandomMedia
		 */
		try (PreparedStatement ps = c.getConnection().prepareStatement("SELECT m.id id_media, m.checksum, m.width, m.height, a.id id_area, a.name area, s.id id_sector, s.name sector, p.id id_problem, p.name problem,"
				+ " ROUND((IFNULL(SUM(nullif(t.grade,-1)),0) + p.grade) / (COUNT(CASE WHEN t.grade>0 THEN t.id END) + 1)) grade,"
				+ " CONCAT('{\"id\":', u.id, ',\"name\":\"', TRIM(CONCAT(u.firstname, ' ', COALESCE(u.lastname,''))), '\"}') photographer," 
				+ " GROUP_CONCAT(DISTINCT CONCAT('{\"id\":', u2.id, ',\"name\":\"', TRIM(CONCAT(u2.firstname, ' ', COALESCE(u2.lastname,''))), '\"}') SEPARATOR ', ') tagged"
				+ " FROM ((((((((media m INNER JOIN media_problem mp ON (m.is_movie=0 AND m.id=mp.media_id AND mp.trivia=0)) INNER JOIN problem p ON mp.problem_id=p.id AND p.locked_admin=0 AND p.locked_superadmin=0) INNER JOIN sector s ON p.sector_id=s.id AND s.locked_admin=0 AND s.locked_superadmin=0) INNER JOIN area a ON s.area_id=a.id AND a.locked_admin=0 AND a.locked_superadmin=0) INNER JOIN region r ON a.region_id=r.id) INNER JOIN user u ON m.photographer_user_id=u.id) LEFT JOIN tick t ON p.id=t.problem_id) LEFT JOIN media_user mu ON m.id=mu.media_id) LEFT JOIN user u2 ON mu.user_id=u2.id"
				+ " WHERE r.id=? AND m.deleted_user_id IS NULL AND a.trash IS NULL AND s.trash IS NULL AND p.trash IS NULL AND a.access_closed IS NULL AND s.access_closed IS NULL"
				+ " GROUP BY m.id, m.checksum, p.id, p.name, m.photographer_user_id, u.firstname, u.lastname"
				+ " ORDER BY rand() LIMIT 1")) {
			ps.setInt(1, setup.getIdRegion());
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					int idMedia = rst.getInt("id_media");
					int crc32 = rst.getInt("checksum");
					int width = rst.getInt("width");
					int height = rst.getInt("height");
					int idArea = rst.getInt("id_area");
					String area = rst.getString("area");
					int idSector = rst.getInt("id_sector");
					String sector = rst.getString("sector");
					int idProblem = rst.getInt("id_problem");
					String problem = rst.getString("problem");
					int grade = rst.getInt("grade");
					String photographerJson = rst.getString("photographer");
					String taggedJson = rst.getString("tagged");
					Frontpage.FrontpageRandomMedia.User photographer = photographerJson == null? null : gson.fromJson(photographerJson, Frontpage.FrontpageRandomMedia.User.class);
					List<Frontpage.FrontpageRandomMedia.User> tagged = taggedJson == null? null : gson.fromJson("[" + taggedJson + "]", new TypeToken<ArrayList<Frontpage.FrontpageRandomMedia.User>>(){}.getType());
					res.setRandomMedia(idMedia, crc32, width, height, idArea, area, idSector, sector, idProblem, problem, setup.getGradeConverter().getGradeFromIdGrade(grade), photographer, tagged);
				}
			}
		}

		logger.debug("getFrontpage(authUserId={}, setup={}) - duration={}", authUserId, setup, stopwatch);
		return res;
	}

	public Collection<GradeDistribution> getGradeDistribution(int authUserId, Setup setup, int optionalAreaId, int optionalSectorId) throws SQLException {
		Map<String, GradeDistribution> res = new LinkedHashMap<>();
		String sqlStr = "WITH x AS ("
				+ "  SELECT g.base_no grade_base_no, x.sorting, x.sector, x.t, COUNT(id_problem) num"
				+ "  FROM (SELECT s.name sector, s.sorting, ty.subtype t, ROUND((IFNULL(SUM(nullif(t.grade,-1)),0) + p.grade) / (COUNT(t.grade) + 1)) grade_id, p.id id_problem"
				+ "    FROM ((((area a INNER JOIN sector s ON a.id=s.area_id) INNER JOIN problem p ON s.id=p.sector_id) INNER JOIN type ty ON p.type_id=ty.id) LEFT JOIN user_region ur ON a.region_id=ur.region_id AND ur.user_id=?) LEFT JOIN tick t ON (p.id=t.problem_id AND t.grade>0)"
				+ (optionalSectorId!=0? " WHERE p.sector_id=?" : " WHERE a.id=?")
				+ "      AND is_readable(ur.admin_read, ur.superadmin_read, p.locked_admin, p.locked_superadmin, p.trash)=1"
				+ "    GROUP BY s.name, ty.subtype, p.id) x, grade g"
				+ "  WHERE x.grade_id=g.grade_id AND g.t=?"
				+ "  GROUP BY x.sorting, x.sector, g.base_no, x.t"
				+ ")"
				+ " SELECT g.base_no grade, x.sector, COALESCE(x.t,'Boulder') t, num"
				+ " FROM (SELECT g.base_no, MIN(g.grade_id) sort FROM grade g WHERE g.t=? GROUP BY g.base_no) g LEFT JOIN x ON g.base_no=x.grade_base_no"
				+ " ORDER BY g.sort, x.sorting, x.sector, x.t";
		try (PreparedStatement ps = c.getConnection().prepareStatement(sqlStr)) {
			ps.setInt(1, authUserId);
			ps.setInt(2, optionalSectorId!=0? optionalSectorId : optionalAreaId);
			ps.setString(3, setup.getGradeSystem().toString());
			ps.setString(4, setup.getGradeSystem().toString());
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					String grade = rst.getString("grade");
					GradeDistribution g = res.get(grade);
					if (g == null) {
						g = new GradeDistribution(grade);
						res.put(grade, g);
					}
					String sector = rst.getString("sector");
					if (sector != null) {
						String t = rst.getString("t");
						int num = rst.getInt("num");
						g.addSector(sector, t, num);
					}
				}
			}
		}
		return res.values();
	}

	public Path getImage(boolean webP, int id) throws SQLException, IOException {
		Path p = null;
		if (webP) {
			p = GlobalFunctions.getPathMediaWebWebp().resolve(String.valueOf(id / 100 * 100)).resolve(id + ".webp");
		} else {
			p = GlobalFunctions.getPathMediaWebJpg().resolve(String.valueOf(id / 100 * 100)).resolve(id + ".jpg");
		}
		Preconditions.checkArgument(Files.exists(p), p.toString() + " does not exist");
		return p;
	}

	public Media getMedia(int authUserId, int id) throws SQLException {
		Media res = null;
		try (PreparedStatement ps = c.getConnection().prepareStatement("SELECT m.id, m.uploader_user_id, m.checksum, m.description, m.width, m.height, m.is_movie, m.embed_url, DATE_FORMAT(m.date_created,'%Y.%m.%d') date_created, DATE_FORMAT(m.date_taken,'%Y.%m.%d') date_taken, TRIM(CONCAT(c.firstname, ' ', COALESCE(c.lastname,''))) capturer, GROUP_CONCAT(DISTINCT TRIM(CONCAT(u.firstname, ' ', COALESCE(u.lastname,''))) ORDER BY u.firstname, u.lastname SEPARATOR ', ') tagged FROM ((media m INNER JOIN user c ON m.photographer_user_id=c.id) LEFT JOIN media_user mu ON m.id=mu.media_id) LEFT JOIN user u ON mu.user_id=u.id WHERE m.id=?")) {
			ps.setInt(1, id);
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					int idMedia = rst.getInt("id");
					int uploaderUserId = rst.getInt("uploader_user_id");
					boolean uploadedByMe = uploaderUserId == authUserId;
					int crc32 = rst.getInt("checksum");
					String description = rst.getString("description");
					String location = null;
					int pitch = 0;
					boolean trivia = false;
					int width = rst.getInt("width");
					int height = rst.getInt("height");
					int tyId = rst.getBoolean("is_movie") ? 2 : 1;
					String embedUrl = rst.getString("embed_url");
					String dateCreated = rst.getString("date_created");
					String dateTaken = rst.getString("date_taken");
					String capturer = rst.getString("capturer");
					String tagged = rst.getString("tagged");
					List<MediaSvgElement> mediaSvgs = getMediaSvgElements(idMedia);
					MediaMetadata mediaMetadata = new MediaMetadata(dateCreated, dateTaken, capturer, tagged, description, location);
					res = new Media(idMedia, uploadedByMe, crc32, pitch, trivia, width, height, tyId, null, mediaSvgs, 0, null, mediaMetadata, embedUrl);
				}
			}
		}
		return res;
	}

	public Point getMediaDimention(int id) throws SQLException {
		Point res = null;
		try (PreparedStatement ps = c.getConnection().prepareStatement("SELECT width, height FROM media WHERE id=?")) {
			ps.setInt(1, id);
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					res = new Point(rst.getInt("width"), rst.getInt("height"));
				}
			}
		}
		return res;
	}

	public List<PermissionUser> getPermissions(int authUserId, int idRegion) throws SQLException {
		ensureSuperadminWriteRegion(authUserId, idRegion);
		// Return users
		List<PermissionUser> res = new ArrayList<>();
		try (PreparedStatement ps = c.getConnection().prepareStatement("SELECT x.id, x.name, x.picture, DATE_FORMAT(MAX(x.last_login),'%Y.%m.%d') last_login, x.admin_read, x.admin_write, x.superadmin_read, x.superadmin_write FROM (SELECT u.id, TRIM(CONCAT(u.firstname, ' ', COALESCE(u.lastname,''))) name, CASE WHEN u.picture IS NOT NULL THEN CONCAT('https://buldreinfo.com/buldreinfo_media/users/', u.id, '.jpg') ELSE '' END picture, MAX(l.when) last_login, ur.admin_read, ur.admin_write, ur.superadmin_read, ur.superadmin_write FROM (user u INNER JOIN user_login l ON u.id=l.user_id) LEFT JOIN user_region ur ON u.id=ur.user_id AND l.region_id=ur.region_id WHERE l.region_id=? GROUP BY u.id, u.firstname, u.lastname, u.picture UNION SELECT u.id, TRIM(CONCAT(u.firstname, ' ', COALESCE(u.lastname,''))) name, CASE WHEN u.picture IS NOT NULL THEN CONCAT('https://buldreinfo.com/buldreinfo_media/users/', u.id, '.jpg') ELSE '' END picture, MAX(l.when) last_login, ur.admin_read, ur.admin_write, ur.superadmin_read, ur.superadmin_write FROM user u, user_region ur, user_login l WHERE u.id=ur.user_id AND ur.region_id=? AND u.id=l.user_id GROUP BY u.id, u.firstname, u.lastname, u.picture) x GROUP BY x.id, x.name, x.picture, x.admin_read, x.admin_write, x.superadmin_read, x.superadmin_write ORDER BY IFNULL(x.superadmin_write,0) DESC, IFNULL(x.superadmin_read,0) DESC, IFNULL(x.admin_write,0) DESC, IFNULL(x.admin_read,0) DESC, x.name")) {
			ps.setInt(1, idRegion);
			ps.setInt(2, idRegion);
			try (ResultSet rst = ps.executeQuery()) {
				final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd");
				while (rst.next()) {
					int userId = rst.getInt("id");
					String name = rst.getString("name");
					String picture = rst.getString("picture");
					String lastLogin = rst.getString("last_login");
					boolean adminRead = rst.getBoolean("admin_read");
					boolean adminWrite = rst.getBoolean("admin_write");
					boolean superadminRead = rst.getBoolean("superadmin_read");
					boolean superadminWrite = rst.getBoolean("superadmin_write");
					String timeAgo = TimeAgo.getTimeAgo(LocalDate.parse(lastLogin, formatter));
					res.add(new PermissionUser(userId, name, picture, timeAgo, adminRead, adminWrite, superadminRead, superadminWrite, authUserId==userId));
				}
			}
		}
		return res;
	}

	public Problem getProblem(int authUserId, Setup s, int reqId, boolean showHiddenMedia) throws IOException, SQLException {
		Stopwatch stopwatch = Stopwatch.createStarted();
		try (PreparedStatement ps = c.getConnection().prepareStatement("UPDATE problem SET hits=hits+1 WHERE id=?")) {
			ps.setInt(1, reqId);
			ps.execute();
		}
		List<Integer> todoIdProblems = new ArrayList<>();
		ProfileTodo todo = getProfileTodo(authUserId, s, authUserId);
		if (todo != null) {
			for (ProfileTodo.ProfileTodoArea ta : todo.getAreas()) {
				for (ProfileTodo.ProfileTodoSector ts : ta.getSectors()) {
					for (ProfileTodo.ProfileTodoProblem tp : ts.getProblems()) {
						todoIdProblems.add(tp.getId());
					}
				}
			}
		}
		Problem p = null;
		String sqlStr = "SELECT a.id area_id, a.locked_admin area_locked_admin, a.locked_superadmin area_locked_superadmin, a.name area_name, a.access_info area_access_info, a.access_closed area_access_closed, a.no_dogs_allowed area_no_dogs_allowed, a.sun_from_hour area_sun_from_hour, a.sun_to_hour area_sun_to_hour, s.id sector_id, s.locked_admin sector_locked_admin, s.locked_superadmin sector_locked_superadmin, s.name sector_name, s.access_info sector_access_info, s.access_closed sector_access_closed, sc.id sector_parking_coordinates_id, sc.latitude sector_parking_latitude, sc.longitude sector_parking_longitude, sc.elevation sector_parking_elevation, sc.elevation_source sector_parking_elevation_source, s.compass_direction_id_calculated sector_compass_direction_id_calculated, s.compass_direction_id_manual sector_compass_direction_id_manual, CONCAT(r.url,'/problem/',p.id) canonical, p.id, p.broken, p.locked_admin, p.locked_superadmin, p.nr, p.name, p.rock, p.description, p.hits, DATE_FORMAT(p.fa_date,'%Y-%m-%d') fa_date, DATE_FORMAT(p.fa_date,'%d/%m-%y') fa_date_hr,"
				+ " ROUND((IFNULL(SUM(nullif(t.grade,-1)),0) + p.grade) / (COUNT(CASE WHEN t.grade>0 THEN t.id END) + 1)) grade, p.grade original_grade, c.id coordinates_id, c.latitude, c.longitude, c.elevation, c.elevation_source,"
				+ " group_concat(DISTINCT CONCAT('{\"id\":', u.id, ',\"name\":\"', TRIM(CONCAT(u.firstname, ' ', COALESCE(u.lastname,''))), '\",\"picture\":\"', CASE WHEN u.picture IS NOT NULL THEN CONCAT('https://buldreinfo.com/buldreinfo_media/users/', u.id, '.jpg') ELSE '' END, '\"}') ORDER BY u.firstname, u.lastname SEPARATOR ',') fa,"
				+ " COUNT(DISTINCT t.id) num_ticks, ROUND(ROUND(AVG(nullif(t.stars,-1))*2)/2,1) stars,"
				+ " MAX(CASE WHEN (t.user_id=? OR u.id=?) THEN 1 END) ticked, ty.id type_id, ty.type, ty.subtype,"
				+ " p.trivia, p.starting_altitude, p.aspect, p.route_length, p.descent"
				+ " FROM ((((((((((area a INNER JOIN region r ON a.region_id=r.id) INNER JOIN region_type rt ON r.id=rt.region_id) INNER JOIN sector s ON a.id=s.area_id) INNER JOIN problem p ON (s.id=p.sector_id AND rt.type_id=p.type_id)) INNER JOIN type ty ON p.type_id=ty.id) LEFT JOIN coordinates sc ON s.parking_coordinates_id=sc.id) LEFT JOIN coordinates c ON p.coordinates_id=c.id) LEFT JOIN fa f ON p.id=f.problem_id) LEFT JOIN user u ON f.user_id=u.id) LEFT JOIN tick t ON p.id=t.problem_id) LEFT JOIN user_region ur ON r.id=ur.region_id AND ur.user_id=?"
				+ " WHERE rt.type_id IN (SELECT type_id FROM region_type WHERE region_id=?)"
				+ "   AND p.id=?"
				+ "   AND is_readable(ur.admin_read, ur.superadmin_read, p.locked_admin, p.locked_superadmin, p.trash)=1"
				+ "   AND (r.id=? OR ur.user_id IS NOT NULL)"
				+ " GROUP BY r.url, a.id, a.locked_admin, a.locked_superadmin, a.name, a.access_info, a.access_closed, a.no_dogs_allowed, a.sun_from_hour, a.sun_to_hour, s.id, s.locked_admin, s.locked_superadmin, s.name, s.access_info, s.access_closed, sc.id, sc.latitude, sc.longitude, sc.elevation, sc.elevation_source, s.compass_direction_id_calculated, s.compass_direction_id_manual, p.id, p.broken, p.locked_admin, p.locked_superadmin, p.nr, p.name, p.rock, p.description, p.hits, p.grade, c.id, c.latitude, c.longitude, c.elevation, c.elevation_source, p.fa_date, ty.id, ty.type, ty.subtype, p.trivia, p.starting_altitude, p.aspect, p.route_length, p.descent"
				+ " ORDER BY p.name";
		try (PreparedStatement ps = c.getConnection().prepareStatement(sqlStr)) {
			ps.setInt(1, authUserId);
			ps.setInt(2, authUserId);
			ps.setInt(3, authUserId);
			ps.setInt(4, s.getIdRegion());
			ps.setInt(5, reqId);
			ps.setInt(6, s.getIdRegion());
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					int areaId = rst.getInt("area_id");
					boolean areaLockedAdmin = rst.getBoolean("area_locked_admin"); 
					boolean areaLockedSuperadmin = rst.getBoolean("area_locked_superadmin");
					String areaName = rst.getString("area_name");
					String areaAccessInfo = rst.getString("area_access_info");
					String areaAccessClosed = rst.getString("area_access_closed");
					boolean areaNoDogsAllowed = rst.getBoolean("area_no_dogs_allowed");
					int areaSunFromHour = rst.getInt("area_sun_from_hour");
					int areaSunToHour = rst.getInt("area_sun_to_hour");
					int sectorId = rst.getInt("sector_id");
					boolean sectorLockedAdmin = rst.getBoolean("sector_locked_admin"); 
					boolean sectorLockedSuperadmin = rst.getBoolean("sector_locked_superadmin");
					String sectorName = rst.getString("sector_name");
					String sectorAccessInfo = rst.getString("sector_access_info");
					String sectorAccessClosed = rst.getString("sector_access_closed");
					int parkingidCoordinates = rst.getInt("sector_parking_coordinates_id");
					Coordinates sectorParking = parkingidCoordinates == 0? null : new Coordinates(parkingidCoordinates, rst.getDouble("sector_parking_latitude"), rst.getDouble("sector_parking_longitude"), rst.getDouble("sector_parking_elevation"), rst.getString("sector_parking_elevation_source"));
					List<Coordinates> sectorOutline = getSectorOutline(sectorId);
					CompassDirection sectorWallDirectionCalculated = getCompassDirection(s, rst.getInt("sector_compass_direction_id_calculated"));
					CompassDirection sectorWallDirectionManual = getCompassDirection(s, rst.getInt("sector_compass_direction_id_manual"));
					Approach sectorApproach = getSectorApproaches(Collections.singleton(sectorId)).getOrDefault(sectorId, null);
					String canonical = rst.getString("canonical");
					int id = rst.getInt("id");
					String broken = rst.getString("broken");
					boolean lockedAdmin = rst.getBoolean("locked_admin");
					boolean lockedSuperadmin = rst.getBoolean("locked_superadmin");
					int nr = rst.getInt("nr");
					int grade = rst.getInt("grade");
					int originalGrade = rst.getInt("original_grade");
					String faDate = rst.getString("fa_date");
					String faDateHr = rst.getString("fa_date_hr");
					String name = rst.getString("name");
					String rock = rst.getString("rock");
					String comment = rst.getString("description");
					String faStr = rst.getString("fa");
					List<FaUser> fa = Strings.isNullOrEmpty(faStr) ? null : gson.fromJson("[" + faStr + "]", new TypeToken<ArrayList<FaUser>>(){}.getType());
					int idCoordinates = rst.getInt("coordinates_id");
					Coordinates coordinates = idCoordinates == 0? null : new Coordinates(idCoordinates, rst.getDouble("latitude"), rst.getDouble("longitude"), rst.getDouble("elevation"), rst.getString("elevation_source"));
					int numTicks = rst.getInt("num_ticks");
					double stars = rst.getDouble("stars");
					boolean ticked = rst.getBoolean("ticked");
					List<Media> media = null;
					List<Media> triviaMedia = null;
					List<Media> allMedia = getMediaProblem(s, authUserId, sectorId, id, showHiddenMedia);
					if (allMedia != null && allMedia.size() > 0) {
						media = allMedia.stream().filter(x -> !x.isTrivia()).collect(Collectors.toList());
						if (media.size() != allMedia.size()) {
							triviaMedia = allMedia.stream().filter(x -> x.isTrivia()).collect(Collectors.toList());
						}
					}
					Type t = new Type(rst.getInt("type_id"), rst.getString("type"), rst.getString("subtype"));
					int hits = rst.getInt("hits");
					String trivia = rst.getString("trivia");
					String startingAltitude = rst.getString("starting_altitude");
					String aspect = rst.getString("aspect");
					String routeLength = rst.getString("route_length");
					String descent = rst.getString("descent");

					SectorProblem neighbourPrev = null;
					SectorProblem neighbourNext = null;
					List<SectorProblem> problems = getSectorProblems(s, authUserId, sectorId);
					if (problems.size() > 1) {
						for (int i = 0; i < problems.size(); i++) {
							SectorProblem prob = problems.get(i);
							if (prob.getId() == id) {
								neighbourPrev = problems.get((i == 0? problems.size()-1 : i-1));
								neighbourNext = problems.get((i == problems.size()-1? 0 : i+1));
								if (neighbourPrev.getId() == neighbourNext.getId()) {
									if (nr < neighbourPrev.getNr()) {
										neighbourPrev = null;
									}
									else {
										neighbourNext = null;
									}
								}
							}
						}
					}

					p = new Problem(null, areaId, areaLockedAdmin, areaLockedSuperadmin, areaName, areaAccessInfo, areaAccessClosed, areaNoDogsAllowed, areaSunFromHour, areaSunToHour,
							sectorId, sectorLockedAdmin, sectorLockedSuperadmin, sectorName, sectorAccessInfo, sectorAccessClosed,
							sectorParking, sectorOutline, sectorWallDirectionCalculated, sectorWallDirectionManual, sectorApproach,
							neighbourPrev, neighbourNext,
							canonical, id, broken, false, lockedAdmin, lockedSuperadmin, nr, name, rock, comment,
							s.getGradeConverter().getGradeFromIdGrade(grade),
							s.getGradeConverter().getGradeFromIdGrade(originalGrade), faDate, faDateHr, fa, coordinates,
							media, numTicks, stars, ticked, null, t, todoIdProblems.contains(id), hits,
							trivia, triviaMedia, startingAltitude, aspect, routeLength, descent);
				}
			}
		}
		if (p == null) {
			// Poblem not found, see if it's visible on a different domain
			Redirect res = c.getBuldreinfoRepo().getCanonicalUrl(0, 0, reqId);
			if (!Strings.isNullOrEmpty(res.getRedirectUrl())) {
				return new Problem(res.getRedirectUrl(), 0, false, false, null, null, null, false, 0, 0, 0, false, false, null, null, null, null, null, null, null, null, null, null, null, 0, null, false, false, false, 0, null, null, null, null, null, null, null, null, null, null, 0, 0, false, null, null, false, 0, null, null, null, null, null, null);
			}
		}

		Preconditions.checkNotNull(p, "Could not find problem with id=" + reqId);
		// Ascents
		Map<Integer, Problem.ProblemTick> tickLookup = new HashMap<>();
		sqlStr = "SELECT t.id id_tick, u.id id_user, CASE WHEN u.picture IS NOT NULL THEN CONCAT('https://buldreinfo.com/buldreinfo_media/users/', u.id, '.jpg') ELSE '' END picture, CAST(t.date AS char) date, CONCAT(u.firstname, ' ', COALESCE(u.lastname,'')) name, t.comment, t.stars, t.grade FROM tick t, user u WHERE t.problem_id=? AND t.user_id=u.id ORDER BY t.date DESC, t.id DESC";
		try (PreparedStatement ps = c.getConnection().prepareStatement(sqlStr)) {
			ps.setInt(1, p.getId());
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					int id = rst.getInt("id_tick");
					int idUser = rst.getInt("id_user");
					String picture = rst.getString("picture");
					String date = rst.getString("date");
					String name = rst.getString("name");
					String comment = rst.getString("comment");
					double stars = rst.getDouble("stars");
					int idGrade = rst.getInt("grade");
					String grade = null;
					boolean noPersonalGrade = false;
					if (idGrade == -1) {
						noPersonalGrade = true;
					}
					else {
						grade = s.getGradeConverter().getGradeFromIdGrade(idGrade);
					}
					boolean writable = idUser == authUserId;
					Problem.ProblemTick t = p.addTick(id, idUser, picture, date, name, grade, noPersonalGrade, comment, stars, writable);
					tickLookup.put(id, t);
				}
			}
		}
		sqlStr = "SELECT r.id, r.tick_id, r.date, r.comment FROM tick t, tick_repeat r WHERE t.problem_id=? AND t.id=r.tick_id ORDER BY r.tick_id, r.date, r.id";
		try (PreparedStatement ps = c.getConnection().prepareStatement(sqlStr)) {
			ps.setInt(1, p.getId());
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					int id = rst.getInt("id");
					int tickId = rst.getInt("tick_id");
					String date = rst.getString("date");
					String comment = rst.getString("comment");
					tickLookup.get(tickId).addRepeat(id, tickId, date, comment);
				}
			}
		}
		// Todos
		sqlStr = "SELECT u.id, CASE WHEN u.picture IS NOT NULL THEN CONCAT('https://buldreinfo.com/buldreinfo_media/users/', u.id, '.jpg') ELSE '' END picture, CONCAT(u.firstname, ' ', COALESCE(u.lastname,'')) name FROM todo t, user u WHERE t.user_id=u.id AND t.problem_id=? ORDER BY u.firstname, u.lastname";
		try (PreparedStatement ps = c.getConnection().prepareStatement(sqlStr)) {
			ps.setInt(1, p.getId());
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					int idUser = rst.getInt("id");
					String picture = rst.getString("picture");
					String name = rst.getString("name");
					p.addTodo(idUser, picture, name);
				}
			}
		}
		// Comments
		try (PreparedStatement ps = c.getConnection().prepareStatement("SELECT g.id, CAST(g.post_time AS char) date, u.id user_id, CASE WHEN u.picture IS NOT NULL THEN CONCAT('https://buldreinfo.com/buldreinfo_media/users/', u.id, '.jpg') ELSE '' END picture, CONCAT(u.firstname, ' ', COALESCE(u.lastname,'')) name, g.message, g.danger, g.resolved FROM guestbook g, user u WHERE g.problem_id=? AND g.user_id=u.id ORDER BY g.post_time DESC")) {
			ps.setInt(1, p.getId());
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					int id = rst.getInt("id");
					String date = rst.getString("date");
					int idUser = rst.getInt("user_id");
					String picture = rst.getString("picture");
					String name = rst.getString("name");
					String message = rst.getString("message");
					boolean danger = rst.getBoolean("danger");
					boolean resolved = rst.getBoolean("resolved");
					List<Media> media = getMediaGuestbook(authUserId, id);
					p.addComment(id, date, idUser, picture, name, message, danger, resolved, media);
				}
				if (p.getComments() != null && !p.getComments().isEmpty()) {
					// Enable editing on last comment in thread if it is written by authenticated user
					Optional<ProblemComment> lastComment = p.getComments().stream().max(Comparator.comparing(ProblemComment::getId));
					if (lastComment.isPresent() && lastComment.get().getIdUser() == authUserId) {
						lastComment.get().setEditable(true);
					}
				}
			}
		}
		// Sections
		try (PreparedStatement ps = c.getConnection().prepareStatement("SELECT id, nr, description, grade FROM problem_section WHERE problem_id=? ORDER BY nr")) {
			ps.setInt(1, p.getId());
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					int id = rst.getInt("id");
					int nr = rst.getInt("nr");
					String description = rst.getString("description");
					int grade = rst.getInt("grade");
					List<Media> sectionMedia = new ArrayList<>();
					if (p.getMedia() != null) {
						sectionMedia = p.getMedia().stream().filter(x -> x.getPitch() == nr).collect(Collectors.toList());
						p.getMedia().removeAll(sectionMedia);
					}
					p.addSection(id, nr, description, s.getGradeConverter().getGradeFromIdGrade(grade), sectionMedia);
				}
			}
		}
		// First aid ascent
		if (!s.isBouldering()) {
			try (PreparedStatement ps = c.getConnection().prepareStatement("SELECT DATE_FORMAT(a.aid_date,'%Y-%m-%d') aid_date, DATE_FORMAT(a.aid_date,'%d/%m-%y') aid_date_hr, a.aid_description, u.id, TRIM(CONCAT(u.firstname, ' ', COALESCE(u.lastname,''))) name, CASE WHEN u.picture IS NOT NULL THEN CONCAT('https://buldreinfo.com/buldreinfo_media/users/', u.id, '.jpg') ELSE '' END picture FROM (fa_aid a LEFT JOIN fa_aid_user au ON a.problem_id=au.problem_id) LEFT JOIN user u ON au.user_id=u.id WHERE a.problem_id=?")) {
				ps.setInt(1, p.getId());
				try (ResultSet rst = ps.executeQuery()) {
					while (rst.next()) {
						String aidDate = rst.getString("aid_date");
						String aidDateHr = rst.getString("aid_date_hr");
						String aidDescription = rst.getString("aid_description");
						FaAid faAid = p.getFaAid();
						if (faAid == null) {
							faAid = new FaAid(p.getId(), aidDate, aidDateHr, aidDescription);
							p.setFaAid(faAid);
						}
						int userId = rst.getInt("id");
						if (userId != 0) {
							String userName = rst.getString("name");
							String picture = rst.getString("picture");
							FaUser user = new FaUser(userId, userName, picture);
							faAid.getUsers().add(user);
						}
					}
				}
			}
		}
		logger.debug("getProblem(authUserId={}, reqRegionId={}, reqId={}) - duration={} - p={}", authUserId, s.getIdRegion(), reqId, stopwatch, p);
		return p;
	}

	public List<ProblemArea> getProblemsList(int authUserId, Setup setup) throws IOException, SQLException {
		Stopwatch stopwatch = Stopwatch.createStarted();
		Map<Integer, ProblemArea> areaLookup = new HashMap<>();
		Map<Integer, ProblemArea.ProblemAreaSector> sectorLookup = new HashMap<>();
		String sqlStr = "SELECT a.id area_id, CONCAT(r.url,'/area/',a.id) area_url, a.name area_name, ac.id area_coordinates_id, ac.latitude area_latitude, ac.longitude area_longitude, ac.elevation area_elevation, ac.elevation_source area_elevation_source, a.locked_admin area_locked_admin, a.locked_superadmin area_locked_superadmin, a.sun_from_hour area_sun_from_hour, a.sun_to_hour area_sun_to_hour, s.id sector_id, CONCAT(r.url,'/sector/',s.id) sector_url, s.name sector_name, s.sorting sector_sorting, sc.id sector_parking_coordinates_id, sc.latitude sector_parking_latitude, sc.longitude sector_parking_longitude, sc.elevation sector_parking_elevation, sc.elevation_source sector_parking_elevation_source, s.compass_direction_id_calculated sector_compass_direction_id_calculated, s.compass_direction_id_manual sector_compass_direction_id_manual, s.locked_admin sector_locked_admin, s.locked_superadmin sector_locked_superadmin, p.id, CONCAT(r.url,'/problem/',p.id) url, p.broken, p.locked_admin, p.locked_superadmin, p.nr, p.name, p.description, c.id coordinates_id, c.latitude, c.longitude, c.elevation, c.elevation_source, ROUND((IFNULL(SUM(nullif(t.grade,-1)),0) + p.grade) / (COUNT(CASE WHEN t.grade>0 THEN t.id END) + 1)) grade,"
				+ " group_concat(DISTINCT CONCAT(TRIM(CONCAT(u.firstname, ' ', COALESCE(u.lastname,'')))) ORDER BY u.firstname, u.lastname SEPARATOR ', ') fa,"
				+ " COUNT(DISTINCT t.id) num_ticks, ROUND(ROUND(AVG(nullif(t.stars,-1))*2)/2,1) stars,"
				+ " MAX(CASE WHEN (t.user_id=? OR u.id=?) THEN 1 END) ticked, ty.id type_id, ty.type, ty.subtype, COUNT(DISTINCT ps.id) num_pitches"
				+ " FROM ((((((((((((area a INNER JOIN region r ON a.region_id=r.id) INNER JOIN region_type rt ON r.id=rt.region_id AND rt.type_id IN (SELECT type_id FROM region_type WHERE region_id=?)) INNER JOIN sector s ON a.id=s.area_id) INNER JOIN problem p ON (s.id=p.sector_id AND rt.type_id=p.type_id)) INNER JOIN type ty ON p.type_id=ty.id) LEFT JOIN coordinates ac ON a.coordinates_id=ac.id) LEFT JOIN coordinates sc ON s.parking_coordinates_id=sc.id) LEFT JOIN coordinates c ON p.coordinates_id=c.id) LEFT JOIN user_region ur ON a.region_id=ur.region_id AND ur.user_id=?) LEFT JOIN fa f ON p.id=f.problem_id) LEFT JOIN user u ON f.user_id=u.id) LEFT JOIN tick t ON p.id=t.problem_id) LEFT JOIN problem_section ps ON p.id=ps.problem_id"
				+ " WHERE (a.region_id=? OR ur.user_id IS NOT NULL)"
				+ " AND is_readable(ur.admin_read, ur.superadmin_read, a.locked_admin, a.locked_superadmin, a.trash)=1"
				+ " AND is_readable(ur.admin_read, ur.superadmin_read, s.locked_admin, s.locked_superadmin, s.trash)=1"
				+ " AND is_readable(ur.admin_read, ur.superadmin_read, p.locked_admin, p.locked_superadmin, p.trash)=1"
				+ " GROUP BY r.url, a.id, a.name, ac.id, ac.latitude, ac.longitude, ac.elevation, ac.elevation_source, a.locked_admin, a.locked_superadmin, a.sun_from_hour, a.sun_to_hour, s.sorting, s.id, s.name, s.sorting, sc.id, sc.latitude, sc.longitude, sc.elevation, sc.elevation_source, s.compass_direction_id_calculated, s.compass_direction_id_manual, s.locked_admin, s.locked_superadmin, p.id, p.broken, p.locked_admin, p.locked_superadmin, p.nr, p.name, p.description, c.id, c.latitude, c.longitude, c.elevation, c.elevation_source, p.grade, ty.id, ty.type, ty.subtype"
				+ " ORDER BY a.name, s.sorting, s.name, p.nr";
		try (PreparedStatement ps = c.getConnection().prepareStatement(sqlStr)) {
			ps.setInt(1, authUserId);
			ps.setInt(2, authUserId);
			ps.setInt(3, setup.getIdRegion());
			ps.setInt(4, authUserId);
			ps.setInt(5, setup.getIdRegion());
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					// Area
					int areaId = rst.getInt("area_id");
					ProblemArea a = areaLookup.get(areaId);
					if (a == null) {
						String areaUrl = rst.getString("area_url");
						String areaName = rst.getString("area_name");
						int areaidCoordinates = rst.getInt("area_coordinates_id");
						Coordinates areaCoordinates = areaidCoordinates == 0? null : new Coordinates(areaidCoordinates, rst.getDouble("area_latitude"), rst.getDouble("area_longitude"), rst.getDouble("area_elevation"), rst.getString("area_elevation_source"));
						boolean areaLockedAdmin = rst.getBoolean("area_locked_admin"); 
						boolean areaLockedSuperadmin = rst.getBoolean("area_locked_superadmin");
						int areaSunFromHour = rst.getInt("area_sun_from_hour");
						int areaSunToHour = rst.getInt("area_sun_to_hour");
						a = new ProblemArea(areaId, areaUrl, areaName, areaCoordinates, areaLockedAdmin, areaLockedSuperadmin, areaSunFromHour, areaSunToHour);
						areaLookup.put(areaId, a);
					}
					// Sector
					int sectorId = rst.getInt("sector_id");
					ProblemArea.ProblemAreaSector s = sectorLookup.get(sectorId);
					if (s == null) {
						String sectorUrl = rst.getString("sector_url");
						String sectorName = rst.getString("sector_name");
						int sectorSorting = rst.getInt("sector_sorting");
						int sectorParkingidCoordinates = rst.getInt("sector_parking_coordinates_id");
						Coordinates sectorParking = sectorParkingidCoordinates == 0? null : new Coordinates(sectorParkingidCoordinates, rst.getDouble("sector_parking_latitude"), rst.getDouble("sector_parking_longitude"), rst.getDouble("sector_parking_elevation"), rst.getString("sector_parking_elevation_source"));
						CompassDirection sectorWallDirectionCalculated = getCompassDirection(setup, rst.getInt("sector_compass_direction_id_calculated"));
						CompassDirection sectorWallDirectionManual = getCompassDirection(setup, rst.getInt("sector_compass_direction_id_manual"));
						boolean sectorLockedAdmin = rst.getBoolean("sector_locked_admin"); 
						boolean sectorLockedSuperadmin = rst.getBoolean("sector_locked_superadmin");
						s = a.addSector(sectorId, sectorUrl, sectorName, sectorSorting, sectorParking, sectorWallDirectionCalculated, sectorWallDirectionManual, sectorLockedAdmin, sectorLockedSuperadmin);
						sectorLookup.put(sectorId, s);
					}
					// Problem
					int id = rst.getInt("id");
					String url = rst.getString("url");
					String broken = rst.getString("broken");
					boolean lockedAdmin = rst.getBoolean("locked_admin");
					boolean lockedSuperadmin = rst.getBoolean("locked_superadmin");
					int nr = rst.getInt("nr");
					String name = rst.getString("name");
					String description = rst.getString("description");
					int idCoordinates = rst.getInt("coordinates_id");
					Coordinates coordinates = idCoordinates == 0? null : new Coordinates(idCoordinates, rst.getDouble("latitude"), rst.getDouble("longitude"), rst.getDouble("elevation"), rst.getString("elevation_source"));
					int grade = rst.getInt("grade");
					String fa = rst.getString("fa");
					int numTicks = rst.getInt("num_ticks");
					double stars = rst.getDouble("stars");
					boolean ticked = rst.getBoolean("ticked");
					Type t = new Type(rst.getInt("type_id"), rst.getString("type"), rst.getString("subtype"));
					int numPitches = rst.getInt("num_pitches");
					s.addProblem(id, url, broken, lockedAdmin, lockedSuperadmin, nr, name, description, coordinates, setup.getGradeConverter().getGradeFromIdGrade(grade), fa, numTicks, stars, ticked, t, numPitches);
				}
			}
		}
		if (!sectorLookup.isEmpty()) {
			// Fill sector outlines
			Multimap<Integer, Coordinates> idSectorOutline = getSectorOutlines(sectorLookup.keySet());
			for (int idSector : idSectorOutline.keySet()) {
				List<Coordinates> outline = Lists.newArrayList(idSectorOutline.get(idSector));
				sectorLookup.get(idSector).setOutline(outline);
			}
		}
		// Sort areas (ae, oe, aa is sorted wrong by MySql):
		List<ProblemArea> res = Lists.newArrayList(areaLookup.values());
		res.sort(Comparator.comparing(ProblemArea::getName));
		res.forEach(a -> a.orderSectors());
		logger.debug("getProblemsList(authUserId={}, setup={}) - res.size()={} - duration={}", authUserId, setup, res.size(), stopwatch);
		return res;
	}

	public Profile getProfile(int authUserId, Setup setup, int reqUserId) throws SQLException {
		int userId = reqUserId > 0? reqUserId : authUserId;
		Preconditions.checkArgument(userId > 0);
		Profile res = null;
		try (PreparedStatement ps = c.getConnection().prepareStatement("SELECT CASE WHEN u.picture IS NOT NULL THEN CONCAT('https://buldreinfo.com/buldreinfo_media/users/', u.id, '.jpg') ELSE '' END picture, u.firstname, u.lastname FROM user u WHERE u.id=?")) {
			ps.setInt(1, userId);
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					String picture = rst.getString("picture");
					String firstname = rst.getString("firstname");
					String lastname = rst.getString("lastname");
					List<UserRegion> userRegions = userId == authUserId? c.getBuldreinfoRepo().getUserRegion(authUserId, setup) : null;
					res = new Profile(userId, picture, firstname, lastname, userRegions);
				}
			}
		}
		return res;
	}

	public List<ProfileMedia> getProfileMediaCapturedArea(int authUserId, Setup setup, int reqId) throws SQLException {
		String sqlStr = "SELECT GROUP_CONCAT(DISTINCT TRIM(CONCAT(u.firstname, ' ', COALESCE(u.lastname,''))) ORDER BY u.firstname, u.lastname SEPARATOR ', ') tagged, m.id, m.uploader_user_id, m.checksum, m.description, MAX(a.name) location, m.width, m.height, m.is_movie, m.embed_url, DATE_FORMAT(m.date_created,'%Y.%m.%d') date_created, DATE_FORMAT(m.date_taken,'%Y.%m.%d') date_taken, 0 pitch, 0 t, TRIM(CONCAT(c.firstname, ' ', COALESCE(c.lastname,''))) capturer, MAX(a.id) area_id FROM ((((((media m INNER JOIN user c ON m.photographer_user_id=? AND m.deleted_user_id IS NULL AND m.photographer_user_id=c.id) INNER JOIN media_area ma ON m.id=ma.media_id) INNER JOIN area a ON ma.area_id=a.id) INNER JOIN region r ON a.region_id=r.id) LEFT JOIN user_region ur ON r.id=ur.region_id AND ur.user_id=?) LEFT JOIN media_user mu ON m.id=mu.media_id) LEFT JOIN user u ON mu.user_id=u.id WHERE is_readable(ur.admin_read, ur.superadmin_read, a.locked_admin, a.locked_superadmin, a.trash)=1 GROUP BY m.id, m.uploader_user_id, m.checksum, m.description, m.width, m.height, m.is_movie, m.embed_url, m.date_created, m.date_taken, c.firstname, c.lastname ORDER BY m.id DESC";
		List<ProfileMedia> res = new ArrayList<>();
		try (PreparedStatement ps = c.getConnection().prepareStatement(sqlStr)) {
			ps.setInt(1, reqId);
			ps.setInt(2, authUserId);
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					String tagged = rst.getString("tagged");
					int idMedia = rst.getInt("id");
					int uploaderUserId = rst.getInt("uploader_user_id");
					boolean uploadedByMe = uploaderUserId == authUserId;
					int crc32 = rst.getInt("checksum");
					String description = rst.getString("description");
					String location = rst.getString("location");
					int pitch = 0;
					boolean trivia = false;
					int width = rst.getInt("width");
					int height = rst.getInt("height");
					int tyId = rst.getBoolean("is_movie") ? 2 : 1;
					String embedUrl = rst.getString("embed_url");
					String dateCreated = rst.getString("date_created");
					String dateTaken = rst.getString("date_taken");
					String capturer = rst.getString("capturer");
					int areaId = rst.getInt("area_id");
					List<MediaSvgElement> mediaSvgs = getMediaSvgElements(idMedia);
					MediaMetadata mediaMetadata = new MediaMetadata(dateCreated, dateTaken, capturer, tagged, description, location);
					String url = "/area/" + areaId;
					ProfileMedia m = new ProfileMedia(idMedia, uploadedByMe, crc32, pitch, trivia, width, height, tyId, null, mediaSvgs, 0, null, mediaMetadata, embedUrl, url);
					res.add(m);
				}
			}
		}
		return res;
	}

	public List<ProfileMedia> getProfileMediaCapturedSector(int authUserId, Setup setup, int reqId) throws SQLException {
		String sqlStr = "SELECT GROUP_CONCAT(DISTINCT TRIM(CONCAT(u.firstname, ' ', COALESCE(u.lastname,''))) ORDER BY u.firstname, u.lastname SEPARATOR ', ') tagged, m.id, m.uploader_user_id, m.checksum, m.description, CONCAT(MAX(s.name),' (',MAX(a.name),')') location, m.width, m.height, m.is_movie, m.embed_url, DATE_FORMAT(m.date_created,'%Y.%m.%d') date_created, DATE_FORMAT(m.date_taken,'%Y.%m.%d') date_taken, 0 pitch, 0 t, TRIM(CONCAT(c.firstname, ' ', COALESCE(c.lastname,''))) capturer, MAX(s.id) sector_id  FROM (((((((media m INNER JOIN user c ON m.photographer_user_id=? AND m.deleted_user_id IS NULL AND m.photographer_user_id=c.id) INNER JOIN media_sector ms ON m.id=ms.media_id) INNER JOIN sector s ON ms.sector_id=s.id) INNER JOIN area a ON s.area_id=a.id) INNER JOIN region r ON a.region_id=r.id) LEFT JOIN user_region ur ON r.id=ur.region_id AND ur.user_id=?) LEFT JOIN media_user mu ON m.id=mu.media_id) LEFT JOIN user u ON mu.user_id=u.id WHERE is_readable(ur.admin_read, ur.superadmin_read, s.locked_admin, s.locked_superadmin, s.trash)=1 GROUP BY m.id, m.uploader_user_id, m.checksum, m.description, m.width, m.height, m.is_movie, m.embed_url, m.date_created, m.date_taken, c.firstname, c.lastname ORDER BY m.id DESC";
		List<ProfileMedia> res = new ArrayList<>();
		try (PreparedStatement ps = c.getConnection().prepareStatement(sqlStr)) {
			ps.setInt(1, reqId);
			ps.setInt(2, authUserId);
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					String tagged = rst.getString("tagged");
					int idMedia = rst.getInt("id");
					int uploaderUserId = rst.getInt("uploader_user_id");
					boolean uploadedByMe = uploaderUserId == authUserId;
					int crc32 = rst.getInt("checksum");
					String description = rst.getString("description");
					String location = rst.getString("location");
					int pitch = 0;
					boolean trivia = false;
					int width = rst.getInt("width");
					int height = rst.getInt("height");
					int tyId = rst.getBoolean("is_movie") ? 2 : 1;
					String embedUrl = rst.getString("embed_url");
					String dateCreated = rst.getString("date_created");
					String dateTaken = rst.getString("date_taken");
					String capturer = rst.getString("capturer");
					int sectorId = rst.getInt("sector_id");
					List<MediaSvgElement> mediaSvgs = getMediaSvgElements(idMedia);
					MediaMetadata mediaMetadata = new MediaMetadata(dateCreated, dateTaken, capturer, tagged, description, location);
					String url = "/sector/" + sectorId;
					ProfileMedia m = new ProfileMedia(idMedia, uploadedByMe, crc32, pitch, trivia, width, height, tyId, null, mediaSvgs, 0, null, mediaMetadata, embedUrl, url);
					res.add(m);
				}
			}
		}
		return res;
	}

	public List<ProfileMedia> getProfileMediaProblem(int authUserId, Setup setup, int reqId, boolean captured) throws SQLException {
		String sqlStr = null;
		if (captured) {
			sqlStr = "SELECT GROUP_CONCAT(DISTINCT TRIM(CONCAT(u.firstname, ' ', COALESCE(u.lastname,''))) ORDER BY u.firstname, u.lastname SEPARATOR ', ') tagged, m.id, m.uploader_user_id, m.checksum, m.description, CONCAT(MAX(p.name),' (',MAX(a.name),'/',MAX(s.name),')') location, m.width, m.height, m.is_movie, m.embed_url, DATE_FORMAT(m.date_created,'%Y.%m.%d') date_created, DATE_FORMAT(m.date_taken,'%Y.%m.%d') date_taken, MAX(mp.pitch) pitch, 0 t, TRIM(CONCAT(c.firstname, ' ', COALESCE(c.lastname,''))) capturer, MAX(p.id) problem_id  FROM ((((((((media m INNER JOIN user c ON m.photographer_user_id=? AND m.deleted_user_id IS NULL AND m.photographer_user_id=c.id) INNER JOIN media_problem mp ON m.id=mp.media_id) INNER JOIN problem p ON mp.problem_id=p.id) INNER JOIN sector s ON p.sector_id=s.id) INNER JOIN area a ON s.area_id=a.id) INNER JOIN region r ON a.region_id=r.id) LEFT JOIN user_region ur ON r.id=ur.region_id AND ur.user_id=?) LEFT JOIN media_user mu ON m.id=mu.media_id) LEFT JOIN user u ON mu.user_id=u.id WHERE is_readable(ur.admin_read, ur.superadmin_read, p.locked_admin, p.locked_superadmin, p.trash)=1 GROUP BY m.id, m.uploader_user_id, m.checksum, m.description, m.width, m.height, m.is_movie, m.embed_url, m.date_created, m.date_taken, c.firstname, c.lastname ORDER BY m.id DESC";
		}
		else {
			sqlStr = "SELECT TRIM(CONCAT(u.firstname, ' ', COALESCE(u.lastname,''))) tagged, m.id, m.uploader_user_id, m.checksum, m.description, CONCAT(MAX(p.name),' (',MAX(a.name),'/',MAX(s.name),')') location, m.width, m.height, m.is_movie, m.embed_url, DATE_FORMAT(m.date_created,'%Y.%m.%d') date_created, DATE_FORMAT(m.date_taken,'%Y.%m.%d') date_taken, mp.pitch, 0 t, TRIM(CONCAT(c.firstname, ' ', COALESCE(c.lastname,''))) capturer, MAX(p.id) problem_id FROM ((((((((user u INNER JOIN media_user mu ON u.id=? AND u.id=mu.user_id) INNER JOIN media m ON mu.media_id=m.id AND m.deleted_user_id IS NULL) INNER JOIN user c ON m.photographer_user_id=c.id) INNER JOIN media_problem mp ON m.id=mp.media_id) INNER JOIN problem p ON mp.problem_id=p.id) INNER JOIN sector s ON p.sector_id=s.id) INNER JOIN area a ON s.area_id=a.id) INNER JOIN region r ON a.region_id=r.id) LEFT JOIN user_region ur ON r.id=ur.region_id AND ur.user_id=? WHERE is_readable(ur.admin_read, ur.superadmin_read, p.locked_admin, p.locked_superadmin, p.trash)=1 GROUP BY u.firstname, u.lastname, u.picture, m.id, m.uploader_user_id, m.checksum, m.description, m.width, m.height, m.is_movie, m.embed_url, m.date_created, m.date_taken, mp.pitch, c.firstname, c.lastname ORDER BY m.id DESC";
		}
		List<ProfileMedia> res = new ArrayList<>();
		try (PreparedStatement ps = c.getConnection().prepareStatement(sqlStr)) {
			ps.setInt(1, reqId);
			ps.setInt(2, authUserId);
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					String tagged = rst.getString("tagged");
					int idMedia = rst.getInt("id");
					int uploaderUserId = rst.getInt("uploader_user_id");
					boolean uploadedByMe = uploaderUserId == authUserId;
					int crc32 = rst.getInt("checksum");
					String description = rst.getString("description");
					String location = rst.getString("location");
					int pitch = 0;
					boolean trivia = false;
					int width = rst.getInt("width");
					int height = rst.getInt("height");
					int tyId = rst.getBoolean("is_movie") ? 2 : 1;
					String embedUrl = rst.getString("embed_url");
					String dateCreated = rst.getString("date_created");
					String dateTaken = rst.getString("date_taken");
					String capturer = rst.getString("capturer");
					int problemId = rst.getInt("problem_id");
					List<MediaSvgElement> mediaSvgs = getMediaSvgElements(idMedia);
					MediaMetadata mediaMetadata = new MediaMetadata(dateCreated, dateTaken, capturer, tagged, description, location);
					String url = "/problem/" + problemId;
					ProfileMedia m = new ProfileMedia(idMedia, uploadedByMe, crc32, pitch, trivia, width, height, tyId, null, mediaSvgs, 0, null, mediaMetadata, embedUrl, url);
					res.add(m);
				}
			}
		}
		return res;
	}

	public ProfileStatistics getProfileStatistics(int authUserId, Setup setup, int reqId) throws SQLException {
		Map<Integer, ProfileStatistics.ProfileStatisticsTick> idProblemTickMap = new HashMap<>();
		ProfileStatistics res = new ProfileStatistics();
		try (PreparedStatement ps = c.getConnection().prepareStatement("SELECT COUNT(DISTINCT CASE WHEN m_a2.is_movie=0 THEN m_a2.id END)+COUNT(DISTINCT CASE WHEN m_s2.is_movie=0 THEN m_s2.id END)+COUNT(DISTINCT CASE WHEN m_p2.is_movie=0 THEN m_p2.id END) num_images_created, COUNT(DISTINCT CASE WHEN m_a2.is_movie=1 THEN m_a2.id END)+COUNT(DISTINCT CASE WHEN m_s2.is_movie=1 THEN m_s2.id END)+COUNT(DISTINCT CASE WHEN m_p2.is_movie=1 THEN m_p2.id END) num_videos_created FROM (((((((((((area a INNER JOIN region r ON a.region_id=r.id) INNER JOIN region_type rt ON r.id=rt.region_id) INNER JOIN user u ON u.id=?) LEFT JOIN media_area m_a ON a.id=m_a.area_id) LEFT JOIN media m_a2 ON m_a.media_id=m_a2.id AND m_a2.deleted_user_id IS NULL AND m_a2.photographer_user_id=u.id) LEFT JOIN sector s ON a.id=s.area_id) LEFT JOIN media_sector m_s ON s.id=m_s.sector_id) LEFT JOIN media m_s2 ON m_s.media_id=m_s2.id AND m_s2.deleted_user_id IS NULL AND m_s2.photographer_user_id=u.id) LEFT JOIN problem p ON s.id=p.sector_id) LEFT JOIN media_problem m_p ON p.id=m_p.problem_id) LEFT JOIN media m_p2 ON m_p.media_id=m_p2.id AND m_p2.deleted_user_id IS NULL AND m_p2.photographer_user_id=u.id) LEFT JOIN user_region ur ON a.region_id=ur.region_id AND ur.user_id=? WHERE rt.type_id IN (SELECT type_id FROM region_type WHERE region_id=?) AND (r.id=? OR ur.user_id IS NOT NULL)")) {
			ps.setInt(1, reqId);
			ps.setInt(2, authUserId);
			ps.setInt(3, setup.getIdRegion());
			ps.setInt(4, setup.getIdRegion());
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					res.setNumImagesCreated(rst.getInt("num_images_created"));
					res.setNumVideosCreated(rst.getInt("num_videos_created"));
				}
			}
		}
		try (PreparedStatement ps = c.getConnection().prepareStatement("SELECT COUNT(DISTINCT CASE WHEN mu_a.user_id IS NOT NULL AND m_a2.is_movie=0 THEN m_a.id END)+COUNT(DISTINCT CASE WHEN mu_s.user_id IS NOT NULL AND m_s2.is_movie=0 THEN m_s.id END)+COUNT(DISTINCT CASE WHEN mu_p.user_id IS NOT NULL AND m_p2.is_movie=0 THEN m_p.id END) num_image_tags, COUNT(DISTINCT CASE WHEN mu_a.user_id IS NOT NULL AND m_a2.is_movie=1 THEN m_a.id END)+COUNT(DISTINCT CASE WHEN mu_s.user_id IS NOT NULL AND m_s2.is_movie=1 THEN m_s.id END)+COUNT(DISTINCT CASE WHEN mu_p.user_id IS NOT NULL AND m_p2.is_movie=1 THEN m_p.id END) num_video_tags FROM ((((((((((((((area a INNER JOIN region r ON a.region_id=r.id) INNER JOIN region_type rt ON r.id=rt.region_id) INNER JOIN user u ON u.id=?) LEFT JOIN media_area m_a ON a.id=m_a.area_id) LEFT JOIN media m_a2 ON m_a.media_id=m_a2.id AND m_a2.deleted_user_id IS NULL) LEFT JOIN media_user mu_a ON m_a2.id=mu_a.media_id AND u.id=mu_a.user_id) LEFT JOIN sector s ON a.id=s.area_id) LEFT JOIN media_sector m_s ON s.id=m_s.sector_id) LEFT JOIN media m_s2 ON m_s.media_id=m_s2.id AND m_s2.deleted_user_id IS NULL) LEFT JOIN media_user mu_s ON m_s2.id=mu_s.media_id AND u.id=mu_s.user_id) LEFT JOIN problem p ON s.id=p.sector_id) LEFT JOIN media_problem m_p ON p.id=m_p.problem_id) LEFT JOIN media m_p2 ON m_p.media_id=m_p2.id AND m_p2.deleted_user_id IS NULL) LEFT JOIN media_user mu_p ON m_p2.id=mu_p.media_id AND u.id=mu_p.user_id) LEFT JOIN user_region ur ON a.region_id=ur.region_id AND ur.user_id=? WHERE rt.type_id IN (SELECT type_id FROM region_type WHERE region_id=?) AND (r.id=? OR ur.user_id IS NOT NULL)")) {
			ps.setInt(1, reqId);
			ps.setInt(2, authUserId);
			ps.setInt(3, setup.getIdRegion());
			ps.setInt(4, setup.getIdRegion());
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					res.setNumImageTags(rst.getInt("num_image_tags"));
					res.setNumVideoTags(rst.getInt("num_video_tags"));
				}
			}
		}

		// Tick
		String sqlStr = "SELECT a.name area_name, a.locked_admin area_locked_admin, a.locked_superadmin area_locked_superadmin, s.name sector_name, s.locked_admin sector_locked_admin, s.locked_superadmin sector_locked_superadmin, t.id id_tick, 0 id_tick_repeat, ty.subtype, COUNT(DISTINCT ps.id) num_pitches, p.id id_problem, p.locked_admin, p.locked_superadmin, p.name, CASE WHEN (t.id IS NOT NULL) THEN t.comment ELSE p.description END comment, DATE_FORMAT(CASE WHEN t.date IS NULL AND f.user_id IS NOT NULL THEN p.fa_date ELSE t.date END,'%Y-%m-%d') date, DATE_FORMAT(CASE WHEN t.date IS NULL AND f.user_id IS NOT NULL THEN p.fa_date ELSE t.date END,'%d/%m-%y') date_hr, t.stars, CASE WHEN (f.user_id IS NOT NULL) THEN f.user_id ELSE 0 END fa, (CASE WHEN t.id IS NOT NULL AND t.grade>=0 THEN t.grade ELSE p.grade END) grade, CASE WHEN t.id IS NOT NULL AND t.grade=-1 THEN 1 ELSE 0 END no_personal_grade"
				+ " FROM ((((((((problem p INNER JOIN type ty ON p.type_id=ty.id) INNER JOIN sector s ON p.sector_id=s.id) INNER JOIN area a ON s.area_id=a.id) INNER JOIN region r ON a.region_id=r.id) INNER JOIN region_type rt ON r.id=rt.region_id) LEFT JOIN problem_section ps ON p.id=ps.problem_id) LEFT JOIN user_region ur ON (r.id=ur.region_id AND ur.user_id=?)) LEFT JOIN tick t ON p.id=t.problem_id AND t.user_id=?) LEFT JOIN fa f ON (p.id=f.problem_id AND f.user_id=?)"
				+ " WHERE (t.user_id IS NOT NULL OR f.user_id IS NOT NULL) AND rt.type_id IN (SELECT type_id FROM region_type WHERE region_id=?) AND (a.region_id=? OR ur.user_id IS NOT NULL) AND is_readable(ur.admin_read, ur.superadmin_read, p.locked_admin, p.locked_superadmin, p.trash)=1"
				+ " GROUP BY a.name, a.locked_admin, a.locked_superadmin, s.name, s.locked_admin, s.locked_superadmin, t.id, ty.subtype, p.id, p.locked_admin, p.locked_superadmin, p.name, p.description, p.fa_date, t.date, t.stars, t.grade, p.grade";
		try (PreparedStatement ps = c.getConnection().prepareStatement(sqlStr)) {
			ps.setInt(1, authUserId);
			ps.setInt(2, reqId);
			ps.setInt(3, reqId);
			ps.setInt(4, setup.getIdRegion());
			ps.setInt(5, setup.getIdRegion());
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					String areaName = rst.getString("area_name");
					boolean areaLockedAdmin = rst.getBoolean("area_locked_admin"); 
					boolean areaLockedSuperadmin = rst.getBoolean("area_locked_superadmin");
					String sectorName = rst.getString("sector_name");
					boolean sectorLockedAdmin = rst.getBoolean("sector_locked_admin"); 
					boolean sectorLockedSuperadmin = rst.getBoolean("sector_locked_superadmin");
					int id = rst.getInt("id_tick");
					int idTickRepeat = rst.getInt("id_tick_repeat");
					String subType = rst.getString("subtype");
					int numPitches = rst.getInt("num_pitches");
					if (numPitches > 1) {
						subType = "Multi-pitch " + subType;
					}
					int idProblem = rst.getInt("id_problem");
					boolean lockedAdmin = rst.getBoolean("locked_admin");
					boolean lockedSuperadmin = rst.getBoolean("locked_superadmin");
					String name = rst.getString("name");
					String comment = rst.getString("comment");
					String date = rst.getString("date");
					String dateHr = rst.getString("date_hr");
					double stars = rst.getDouble("stars");
					boolean fa = rst.getBoolean("fa");
					int grade = rst.getInt("grade");
					boolean noPersonalGrade = rst.getBoolean("no_personal_grade");
					ProfileStatistics.ProfileStatisticsTick tick = res.addTick(areaName, areaLockedAdmin, areaLockedSuperadmin, sectorName, sectorLockedAdmin, sectorLockedSuperadmin, id, idTickRepeat, subType, numPitches, idProblem, lockedAdmin, lockedSuperadmin, name, comment, date, dateHr, stars, fa, setup.getGradeConverter().getGradeFromIdGrade(grade), grade, noPersonalGrade);
					idProblemTickMap.put(idProblem, tick);
				}
			}
		}
		// Tick_repeat
		sqlStr = "SELECT a.name area_name, a.locked_admin area_locked_admin, a.locked_superadmin area_locked_superadmin, s.name sector_name, s.locked_admin sector_locked_admin, s.locked_superadmin sector_locked_superadmin, t.id id_tick, tr.id id_tick_repeat, ty.subtype, COUNT(DISTINCT ps.id) num_pitches, p.id id_problem, p.locked_admin, p.locked_superadmin, p.name, tr.comment, DATE_FORMAT(tr.date,'%Y-%m-%d') date, DATE_FORMAT(tr.date,'%d/%m-%y') date_hr, t.stars, 0 fa, t.grade"
				+ " FROM ((((((((problem p INNER JOIN type ty ON p.type_id=ty.id) INNER JOIN sector s ON p.sector_id=s.id) INNER JOIN area a ON s.area_id=a.id) INNER JOIN region r ON a.region_id=r.id) INNER JOIN region_type rt ON r.id=rt.region_id) INNER JOIN tick t ON p.id=t.problem_id AND t.user_id=?) INNER JOIN tick_repeat tr ON t.id=tr.tick_id) LEFT JOIN problem_section ps ON p.id=ps.problem_id) LEFT JOIN user_region ur ON (r.id=ur.region_id AND ur.user_id=?)"
				+ " WHERE rt.type_id IN (SELECT type_id FROM region_type WHERE region_id=?) AND (a.region_id=? OR ur.user_id IS NOT NULL) AND is_readable(ur.admin_read, ur.superadmin_read, p.locked_admin, p.locked_superadmin, p.trash)=1"
				+ " GROUP BY a.name, a.locked_admin, a.locked_superadmin, s.name, s.locked_admin, s.locked_superadmin, t.id, tr.id, ty.subtype, p.id, p.locked_admin, p.locked_superadmin, p.name, tr.comment, tr.date, t.stars, t.grade";
		try (PreparedStatement ps = c.getConnection().prepareStatement(sqlStr)) {
			ps.setInt(1, reqId);
			ps.setInt(2, authUserId);
			ps.setInt(3, setup.getIdRegion());
			ps.setInt(4, setup.getIdRegion());
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					String areaName = rst.getString("area_name");
					boolean areaLockedAdmin = rst.getBoolean("area_locked_admin"); 
					boolean areaLockedSuperadmin = rst.getBoolean("area_locked_superadmin");
					String sectorName = rst.getString("sector_name");
					boolean sectorLockedAdmin = rst.getBoolean("sector_locked_admin"); 
					boolean sectorLockedSuperadmin = rst.getBoolean("sector_locked_superadmin");
					int id = rst.getInt("id_tick");
					int idTickRepeat = rst.getInt("id_tick_repeat");
					String subType = rst.getString("subtype");
					int numPitches = rst.getInt("num_pitches");
					if (numPitches > 1) {
						subType = "Multi-pitch " + subType;
					}
					int idProblem = rst.getInt("id_problem");
					boolean lockedAdmin = rst.getBoolean("locked_admin");
					boolean lockedSuperadmin = rst.getBoolean("locked_superadmin");
					String name = rst.getString("name");
					String comment = rst.getString("comment");
					String date = rst.getString("date");
					String dateHr = rst.getString("date_hr");
					double stars = rst.getDouble("stars");
					boolean fa = rst.getBoolean("fa");
					int grade = rst.getInt("grade");
					boolean noPersonalGrade = false;
					res.addTick(areaName, areaLockedAdmin, areaLockedSuperadmin, sectorName, sectorLockedAdmin, sectorLockedSuperadmin, id, idTickRepeat, subType, numPitches, idProblem, lockedAdmin, lockedSuperadmin, name, comment, date, dateHr, stars, fa, setup.getGradeConverter().getGradeFromIdGrade(grade), grade, noPersonalGrade);
				}
			}
		}
		// First aid ascent
		if (!setup.isBouldering()) {
			try (PreparedStatement ps = c.getConnection().prepareStatement("SELECT a.name area_name, a.locked_admin area_locked_admin, a.locked_superadmin area_locked_superadmin, s.name sector_name, s.locked_admin sector_locked_admin, s.locked_superadmin sector_locked_superadmin, COUNT(DISTINCT ps.id) num_pitches, p.id id_problem, p.locked_admin, p.locked_superadmin, p.name, aid.aid_description description, DATE_FORMAT(aid.aid_date,'%Y-%m-%d') date, DATE_FORMAT(aid.aid_date,'%d/%m-%y') date_hr" +
					" FROM (((((((problem p INNER JOIN sector s ON p.sector_id=s.id) INNER JOIN area a ON s.area_id=a.id) INNER JOIN region r ON a.region_id=r.id) INNER JOIN region_type rt ON r.id=rt.region_id) INNER JOIN fa_aid aid ON p.id=aid.problem_id) INNER JOIN fa_aid_user aid_u ON (p.id=aid_u.problem_id AND aid_u.user_id=?) LEFT JOIN problem_section ps ON p.id=ps.problem_id) LEFT JOIN user_region ur ON (r.id=ur.region_id AND ur.user_id=?))" + 
					" WHERE rt.type_id IN (SELECT type_id FROM region_type WHERE region_id=?) AND (a.region_id=? OR ur.user_id IS NOT NULL) AND is_readable(ur.admin_read, ur.superadmin_read, p.locked_admin, p.locked_superadmin, p.trash)=1" + 
					" GROUP BY a.name, a.locked_admin, a.locked_superadmin, s.name, s.locked_admin, s.locked_superadmin, p.id, p.locked_admin, p.locked_superadmin, p.name, aid.aid_description, aid.aid_date")) {
				ps.setInt(1, reqId);
				ps.setInt(2, authUserId);
				ps.setInt(3, setup.getIdRegion());
				ps.setInt(4, setup.getIdRegion());
				try (ResultSet rst = ps.executeQuery()) {
					while (rst.next()) {
						String areaName = rst.getString("area_name");
						boolean areaLockedAdmin = rst.getBoolean("area_locked_admin"); 
						boolean areaLockedSuperadmin = rst.getBoolean("area_locked_superadmin");
						String sectorName = rst.getString("sector_name");
						boolean sectorLockedAdmin = rst.getBoolean("sector_locked_admin"); 
						boolean sectorLockedSuperadmin = rst.getBoolean("sector_locked_superadmin");
						int numPitches = rst.getInt("num_pitches");
						int idProblem = rst.getInt("id_problem");
						boolean lockedAdmin = rst.getBoolean("locked_admin");
						boolean lockedSuperadmin = rst.getBoolean("locked_superadmin");
						String name = rst.getString("name");
						String comment = rst.getString("description");
						if (!Strings.isNullOrEmpty(comment)) {
							comment = "First ascent (AID): " + comment;
						}
						else {
							comment = "First ascent (AID)";
						}
						String date = rst.getString("date");
						String dateHr = rst.getString("date_hr");
						int grade = 0;
						boolean noPersonalGrade = false;
						ProfileStatistics.ProfileStatisticsTick tick = res.addTick(areaName, areaLockedAdmin, areaLockedSuperadmin, sectorName, sectorLockedAdmin, sectorLockedSuperadmin, 0, 0, "Aid", numPitches, idProblem, lockedAdmin, lockedSuperadmin, name, comment, date, dateHr, 0, true, setup.getGradeConverter().getGradeFromIdGrade(grade), grade, noPersonalGrade);
						idProblemTickMap.put(idProblem, tick);
					}
				}
			}
		}
		if (!idProblemTickMap.isEmpty()) {
			// Fill coordinates
			Map<Integer, Coordinates> idProblemCoordinates = getProblemCoordinates(idProblemTickMap.keySet());
			for (int idProblem : idProblemCoordinates.keySet()) {
				Coordinates coordinates = idProblemCoordinates.get(idProblem);
				idProblemTickMap.get(idProblem).setCoordinates(coordinates);
			}
		}
		// Order ticks
		res.getTicks().sort((t1, t2) -> -ComparisonChain
				.start()
				.compare(Strings.nullToEmpty(t1.getDate()), Strings.nullToEmpty(t2.getDate()))
				.compare(t1.getId(), t2.getId())
				.compare(t1.getIdProblem(), t2.getIdProblem())
				.result());
		for (int i = 0; i < res.getTicks().size(); i++) {
			res.getTicks().get(i).setNum(i);
		}
		return res;
	}

	public ProfileTodo getProfileTodo(int authUserId, Setup setup, int reqId) throws SQLException {
		final int userId = reqId > 0? reqId : authUserId;
		ProfileTodo res = new ProfileTodo();

		// Build lists
		Map<Integer, ProfileTodo.ProfileTodoArea> areaLookup = new HashMap<>();
		Map<Integer, ProfileTodo.ProfileTodoSector> sectorLookup = new HashMap<>();
		Map<Integer, ProfileTodo.ProfileTodoProblem> problemLookup = new HashMap<>();
		try (PreparedStatement ps = c.getConnection().prepareStatement("SELECT a.id area_id, CONCAT(r.url,'/area/',a.id) area_url, a.name area_name, a.locked_admin area_locked_admin, a.locked_superadmin area_locked_superadmin, s.id sector_id, CONCAT(r.url,'/sector/',s.id) sector_url, s.name sector_name, s.locked_admin sector_locked_admin, s.locked_superadmin sector_locked_superadmin, t.id todo_id, p.id problem_id, CONCAT(r.url,'/problem/',p.id) problem_url, p.nr problem_nr, p.name problem_name, p.grade problem_grade, p.locked_admin problem_locked_admin, p.locked_superadmin problem_locked_superadmin FROM (((((area a INNER JOIN region r ON a.region_id=r.id) INNER JOIN region_type rt ON r.id=rt.region_id) INNER JOIN sector s ON a.id=s.area_id) INNER JOIN problem p ON s.id=p.sector_id) LEFT JOIN todo t ON p.id=t.problem_id) LEFT JOIN user_region ur ON r.id=ur.region_id AND ur.user_id=? WHERE rt.type_id IN (SELECT type_id FROM region_type WHERE region_id=?) AND (r.id=? OR ur.user_id IS NOT NULL) AND t.user_id=? AND is_readable(ur.admin_read, ur.superadmin_read, p.locked_admin, p.locked_superadmin, p.trash)=1 GROUP BY r.url, t.id, a.id, a.name, a.locked_admin, a.locked_superadmin, s.id, s.locked_admin, s.locked_superadmin, s.name, p.id, p.nr, p.name, p.grade, p.locked_admin, p.locked_superadmin ORDER BY a.name, s.name, p.nr")) {
			ps.setInt(1, authUserId);
			ps.setInt(2, setup.getIdRegion());
			ps.setInt(3, setup.getIdRegion());
			ps.setInt(4, userId);
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					// Area
					int areaId = rst.getInt("area_id");
					ProfileTodo.ProfileTodoArea a = areaLookup.get(areaId);
					if (a == null) {
						String areaUrl = rst.getString("area_url");
						String areaName = rst.getString("area_name");
						boolean areaLockedAdmin = rst.getBoolean("area_locked_admin"); 
						boolean areaLockedSuperadmin = rst.getBoolean("area_locked_superadmin");
						a = res.addArea(areaId, areaUrl, areaName, areaLockedAdmin, areaLockedSuperadmin);
						areaLookup.put(areaId, a);
					}
					// Sector
					int sectorId = rst.getInt("sector_id");
					ProfileTodo.ProfileTodoSector s = sectorLookup.get(sectorId);
					if (s == null) {
						String sectorUrl = rst.getString("sector_url");
						String sectorName = rst.getString("sector_name");
						boolean sectorLockedAdmin = rst.getBoolean("sector_locked_admin"); 
						boolean sectorLockedSuperadmin = rst.getBoolean("sector_locked_superadmin");
						s = a.addSector(sectorId, sectorUrl, sectorName, sectorLockedAdmin, sectorLockedSuperadmin);
						sectorLookup.put(sectorId, s);
					}
					// Problem
					int todoId = rst.getInt("todo_id");
					int problemId = rst.getInt("problem_id");
					String problemUrl = rst.getString("problem_url");
					int problemNr = rst.getInt("problem_nr");
					String problemName = rst.getString("problem_name");
					int problemGrade = rst.getInt("problem_grade");
					boolean problemLockedAdmin = rst.getBoolean("problem_locked_admin");
					boolean problemLockedSuperadmin = rst.getBoolean("problem_locked_superadmin");
					ProfileTodo.ProfileTodoProblem p = s.addProblem(todoId, problemId, problemUrl, problemLockedAdmin, problemLockedSuperadmin, problemNr, problemName, setup.getGradeConverter().getGradeFromIdGrade(problemGrade));
					problemLookup.put(problemId, p);
				}
			}
		}
		if (!problemLookup.isEmpty()) {
			String problemIds = Joiner.on(",").join(problemLookup.keySet());
			String sqlStr = String.format("SELECT t.problem_id, u.id, TRIM(CONCAT(u.firstname, ' ', COALESCE(u.lastname,''))) name FROM todo t, user u WHERE t.user_id=u.id AND t.user_id!=? AND problem_id IN (%s) ORDER BY t.problem_id, u.firstname, u.lastname", problemIds);
			try (PreparedStatement ps = c.getConnection().prepareStatement(sqlStr)) {
				ps.setInt(1, userId);
				try (ResultSet rst = ps.executeQuery()) {
					while (rst.next()) {
						int problemId = rst.getInt("problem_id");
						int id = rst.getInt("id");
						String name = rst.getString("name");
						problemLookup.get(problemId).addPartner(id, name);
					}
				}
			}
			if (!problemLookup.isEmpty()) {
				// Fill coordinates
				Map<Integer, Coordinates> idProblemCoordinates = getProblemCoordinates(problemLookup.keySet());
				for (int idProblem : idProblemCoordinates.keySet()) {
					Coordinates coordinates = idProblemCoordinates.get(idProblem);
					problemLookup.get(idProblem).setCoordinates(coordinates);
				}
			}
		}
		// Sort areas (ae, oe, aa is sorted wrong by MySql):
		res.getAreas().sort(Comparator.comparing(ProfileTodo.ProfileTodoArea::getName));
		logger.debug("getProfileTodo(authUserId={}, idRegion={}, reqId={}) - res={}", authUserId, setup.getIdRegion(), reqId, res);
		return res;
	}

	public Collection<V1Region> getRegions(String uniqueId, boolean climbingNotBouldering) throws SQLException {
		final String regionTypeFilter = climbingNotBouldering? "rt.type_id!=1" : "rt.type_id=1";
		final int idUser = upsertUserReturnId(uniqueId);
		Map<Integer, V1Region> regionMap = new HashMap<>();
		Map<Integer, com.buldreinfo.jersey.jaxb.model.v1.V1Area> areaMap = new HashMap<>();
		Map<Integer, com.buldreinfo.jersey.jaxb.model.v1.V1Sector> sectorMap = new HashMap<>();
		Map<Integer, com.buldreinfo.jersey.jaxb.model.v1.V1Problem> problemMap = new HashMap<>();
		// Regions
		try (PreparedStatement ps = c.getConnection().prepareStatement("SELECT r.id, r.name FROM region r INNER JOIN region_type rt ON r.id=rt.region_id WHERE " + regionTypeFilter)) {
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					int id = rst.getInt("id");
					String name = rst.getString("name");
					V1Region r = new V1Region(id, name);
					regionMap.put(r.getId(), r);
				}
			}
		}
		// Areas
		try (PreparedStatement ps = c.getConnection().prepareStatement("SELECT a.region_id, a.id, a.name, a.description, c.latitude, c.longitude FROM (((area a INNER JOIN region r ON a.region_id=r.id) INNER JOIN region_type rt ON r.id=rt.region_id) LEFT JOIN coordinates c ON a.coordinates_id=c.id) LEFT JOIN user_region ur ON a.region_id=ur.region_id AND ur.user_id=? WHERE " + regionTypeFilter + " AND is_readable(ur.admin_read, ur.superadmin_read, a.locked_admin, a.locked_superadmin, a.trash)=1 GROUP BY a.region_id, a.id, a.name, a.description, c.latitude, c.longitude")) {
			ps.setInt(1, idUser);
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					int regionId = rst.getInt("region_id");
					V1Region r = regionMap.get(regionId);
					if (r != null) {
						int id = rst.getInt("id");
						String name = rst.getString("name");
						String comment = rst.getString("description");
						double latitude = rst.getDouble("latitude");
						double longitude = rst.getDouble("longitude");
						com.buldreinfo.jersey.jaxb.model.v1.V1Area a = new com.buldreinfo.jersey.jaxb.model.v1.V1Area(regionId, id, name, comment, latitude, longitude);
						r.getAreas().add(a);
						areaMap.put(a.getId(), a);
					}
				}
			}
		}
		// Sectors
		try (PreparedStatement ps = c.getConnection().prepareStatement("SELECT s.area_id, s.id, s.name, s.description, c.latitude, c.longitude FROM ((((sector s INNER JOIN area a ON a.id=s.area_id) INNER JOIN region r ON a.region_id=r.id) INNER JOIN region_type rt ON r.id=rt.region_id) LEFT JOIN coordinates c ON s.parking_coordinates_id=c.id) LEFT JOIN user_region ur ON r.id=ur.region_id AND ur.user_id=? WHERE " + regionTypeFilter + " AND is_readable(ur.admin_read, ur.superadmin_read, s.locked_admin, s.locked_superadmin, s.trash)=1 GROUP BY s.area_id, s.id, s.name, s.description, c.latitude, c.longitude")) {
			ps.setInt(1, idUser);
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					int areaId = rst.getInt("area_id");
					com.buldreinfo.jersey.jaxb.model.v1.V1Area a = areaMap.get(areaId);
					if (a != null) {
						int id = rst.getInt("id");
						String name = rst.getString("name");
						String comment = rst.getString("description");
						double latitude = rst.getDouble("latitude");
						double longitude = rst.getDouble("longitude");
						com.buldreinfo.jersey.jaxb.model.v1.V1Sector s = new com.buldreinfo.jersey.jaxb.model.v1.V1Sector(areaId, id, name, comment, latitude, longitude);
						a.getSectors().add(s);
						sectorMap.put(s.getId(), s);
					}
				}
			}
		}
		// Problems
		try (PreparedStatement ps = c.getConnection().prepareStatement("SELECT p.sector_id, p.id, p.nr, p.name, p.description, p.grade, TRIM(CONCAT(IFNULL(p.fa_date,''), ' ', GROUP_CONCAT(DISTINCT CONCAT(u.firstname, ' ', COALESCE(u.lastname,'')) ORDER BY u.firstname SEPARATOR ', '))) fa, c.latitude, c.longitude FROM (((((((area a INNER JOIN region r ON a.region_id=r.id) INNER JOIN region_type rt ON r.id=rt.region_id) INNER JOIN sector s ON a.id=s.area_id) INNER JOIN problem p ON s.id=p.sector_id) LEFT JOIN coordinates c ON p.coordinates_id=c.id) LEFT JOIN fa f ON p.id=f.problem_id) LEFT JOIN user u ON f.user_id=u.id) LEFT JOIN user_region ur ON r.id=ur.region_id AND ur.user_id=? WHERE " + regionTypeFilter + " AND is_readable(ur.admin_read, ur.superadmin_read, p.locked_admin, p.locked_superadmin, p.trash)=1 GROUP BY p.sector_id, p.id, p.nr, p.name, p.description, p.grade, p.fa_date, c.latitude, c.longitude")) {
			ps.setInt(1, idUser);
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					int sectorId = rst.getInt("sector_id");
					com.buldreinfo.jersey.jaxb.model.v1.V1Sector s = sectorMap.get(sectorId);
					if (s != null) {
						int id = rst.getInt("id");
						int nr = rst.getInt("nr");
						String name = rst.getString("name");
						String comment = rst.getString("description");
						int grade = rst.getInt("grade");
						String fa = rst.getString("fa");
						double latitude = rst.getDouble("latitude");
						double longitude = rst.getDouble("longitude");
						com.buldreinfo.jersey.jaxb.model.v1.V1Problem p = new com.buldreinfo.jersey.jaxb.model.v1.V1Problem(sectorId, id, nr, name, comment, grade, fa, latitude, longitude);
						s.getProblems().add(p);
						problemMap.put(p.getId(), p);
					}
				}
			}
		}
		// Media (sectors)
		try (PreparedStatement ps = c.getConnection().prepareStatement("SELECT ms.sector_id, m.id, m.is_movie FROM (((((media m INNER JOIN media_sector ms ON m.id=ms.media_id AND m.deleted_user_id IS NULL AND m.embed_url IS NULL) INNER JOIN sector s ON ms.sector_id=s.id) INNER JOIN area a ON s.area_id=a.id) INNER JOIN region r ON a.region_id=r.id) INNER JOIN region_type rt ON r.id=rt.region_id) LEFT JOIN user_region ur ON a.region_id=ur.region_id AND ur.user_id=? WHERE " + regionTypeFilter + " AND is_readable(ur.admin_read, ur.superadmin_read, s.locked_admin, s.locked_superadmin, s.trash)=1 AND m.id NOT IN (SELECT media_id FROM svg) GROUP BY ms.sector_id, m.id, m.is_movie ORDER BY m.is_movie, m.id")) {
			ps.setInt(1, idUser);
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					int sectorId = rst.getInt("sector_id");
					com.buldreinfo.jersey.jaxb.model.v1.V1Sector s = sectorMap.get(sectorId);
					if (s != null) {
						int id = rst.getInt("id");
						boolean isMovie = rst.getBoolean("is_movie");
						s.getMedia().add(new com.buldreinfo.jersey.jaxb.model.v1.V1Media(id, isMovie, 0));
					}
				}
			}
		}
		// Media (problems)
		try (PreparedStatement ps = c.getConnection().prepareStatement("SELECT mp.problem_id, m.id, m.is_movie, mp.milliseconds t FROM ((((((media m INNER JOIN media_problem mp ON m.id=mp.media_id AND m.deleted_user_id IS NULL AND m.embed_url IS NULL) INNER JOIN problem p ON mp.problem_id=p.id) INNER JOIN sector s ON p.sector_id=s.id) INNER JOIN area a ON s.area_id=a.id) INNER JOIN region r ON a.region_id=r.id) INNER JOIN region_type rt ON r.id=rt.region_id) LEFT JOIN user_region ur ON r.id=ur.region_id AND ur.user_id=? WHERE " + regionTypeFilter + " AND is_readable(ur.admin_read, ur.superadmin_read, p.locked_admin, p.locked_superadmin, p.trash)=1 GROUP BY mp.problem_id, m.id, m.is_movie, mp.milliseconds ORDER BY m.is_movie, m.id")) {
			ps.setInt(1, idUser);
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					int problemId = rst.getInt("problem_id");
					com.buldreinfo.jersey.jaxb.model.v1.V1Problem p = problemMap.get(problemId);
					if (p != null) {
						int id = rst.getInt("id");
						boolean isMovie = rst.getBoolean("is_movie");
						int t = rst.getInt("t");
						p.getMedia().add(new com.buldreinfo.jersey.jaxb.model.v1.V1Media(id, isMovie, t));
					}
				}
			}
		}
		// Return
		return regionMap.values();
	}

	public List<Search> getSearch(int authUserId, Setup setup, String search) throws SQLException {
		List<Search> res = new ArrayList<>();
		// Areas
		Set<Integer> areaIdsVisible = new HashSet<>();
		List<Search> areas = new ArrayList<>();
		try (PreparedStatement ps = c.getConnection().prepareStatement("SELECT a.id, a.name, a.locked_admin, a.locked_superadmin, MAX(m.id) media_id, MAX(m.checksum) media_crc32 FROM ((((area a INNER JOIN region r ON a.region_id=r.id) INNER JOIN region_type rt ON r.id=rt.region_id) LEFT JOIN user_region ur ON r.id=ur.region_id AND ur.user_id=?) LEFT JOIN media_area ma ON a.id=ma.area_id) LEFT JOIN media m ON ma.media_id=m.id AND m.is_movie=0 AND m.deleted_user_id IS NULL WHERE rt.type_id IN (SELECT type_id FROM region_type WHERE region_id=?) AND (r.id=? OR ur.user_id IS NOT NULL) AND (a.name LIKE ? OR a.name LIKE ? OR a.name LIKE ?) AND is_readable(ur.admin_read, ur.superadmin_read, a.locked_admin, a.locked_superadmin, a.trash)=1 GROUP BY a.id, a.name, a.locked_admin, a.locked_superadmin, a.hits ORDER BY a.hits DESC, a.name LIMIT 8")) {
			ps.setInt(1, authUserId);
			ps.setInt(2, setup.getIdRegion());
			ps.setInt(3, setup.getIdRegion());
			ps.setString(4, search + "%");
			ps.setString(5, "% " + search + "%");
			ps.setString(6, "%(" + search + "%");
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					int id = rst.getInt("id");
					areaIdsVisible.add(id);
					String name = rst.getString("name");
					boolean lockedAdmin = rst.getBoolean("locked_admin");
					boolean lockedSuperadmin = rst.getBoolean("locked_superadmin");
					int mediaId = rst.getInt("media_id");
					int mediaCrc32 = rst.getInt("media_crc32");
					areas.add(new Search(name, null, "/area/" + id, null, null, mediaId, mediaCrc32, lockedAdmin, lockedSuperadmin));
				}
			}
		}
		// External Areas
		List<Search> externalAreas = new ArrayList<>();
		try (PreparedStatement ps = c.getConnection().prepareStatement("SELECT a_external.id, CONCAT(r_external.url,'/area/',a_external.id) external_url, a_external.name, r_external.name region_name FROM region r, region_type rt, region_type rt_external, region r_external, area a_external WHERE r.id=? AND r.id=rt.region_id AND rt.type_id=rt_external.type_id AND rt_external.region_id=r_external.id AND r.id!=r_external.id AND r_external.id=a_external.region_id AND a_external.locked_admin=0 AND a_external.locked_superadmin=0 AND (a_external.name LIKE ? OR a_external.name LIKE ? OR a_external.name LIKE ?) GROUP BY r_external.url, a_external.id, a_external.name, r_external.name, a_external.hits ORDER BY a_external.hits DESC, a_external.name LIMIT 3")) {
			ps.setInt(1, setup.getIdRegion());
			ps.setString(2, search + "%");
			ps.setString(3, "% " + search + "%");
			ps.setString(4, "%(" + search + "%");
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					int id = rst.getInt("id");
					if (!areaIdsVisible.contains(id)) {
						String externalUrl = rst.getString("external_url");
						String name = rst.getString("name");
						String regionName = rst.getString("region_name");
						externalAreas.add(new Search(name, regionName, null, externalUrl, null, 0, 0, false, false));
					}
				}
			}
		}
		// Sectors
		List<Search> sectors = new ArrayList<>();
		try (PreparedStatement ps = c.getConnection().prepareStatement("SELECT s.id, a.name area_name, s.name sector_name, s.locked_admin, s.locked_superadmin, MAX(m.id) media_id, MAX(m.checksum) media_crc32 FROM (((((area a INNER JOIN region r ON a.region_id=r.id) INNER JOIN region_type rt ON r.id=rt.region_id) INNER JOIN sector s ON a.id=s.area_id) LEFT JOIN user_region ur ON r.id=ur.region_id AND ur.user_id=?) LEFT JOIN media_sector ms ON s.id=ms.sector_id) LEFT JOIN media m ON ms.media_id=m.id AND m.is_movie=0 AND m.deleted_user_id IS NULL WHERE rt.type_id IN (SELECT type_id FROM region_type WHERE region_id=?) AND (r.id=? OR ur.user_id IS NOT NULL) AND (s.name LIKE ? OR s.name LIKE ?) AND is_readable(ur.admin_read, ur.superadmin_read, s.locked_admin, s.locked_superadmin, s.trash)=1 GROUP BY s.id, a.name, s.name, s.locked_admin, s.locked_superadmin, s.hits ORDER BY s.hits DESC, a.name, s.name LIMIT 8")) {
			ps.setInt(1, authUserId);
			ps.setInt(2, setup.getIdRegion());
			ps.setInt(3, setup.getIdRegion());
			ps.setString(4, search + "%");
			ps.setString(5, "% " + search + "%");
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					int id = rst.getInt("id");
					String areaName = rst.getString("area_name");
					String sectorName = rst.getString("sector_name");
					boolean lockedAdmin = rst.getBoolean("locked_admin");
					boolean lockedSuperadmin = rst.getBoolean("locked_superadmin");
					int mediaId = rst.getInt("media_id");
					int mediaCrc32 = rst.getInt("media_crc32");
					sectors.add(new Search(sectorName, areaName, "/sector/" + id, null, null, mediaId, mediaCrc32, lockedAdmin, lockedSuperadmin));
				}
			}
		}
		// Problems
		List<Search> problems = new ArrayList<>();
		try (PreparedStatement ps = c.getConnection().prepareStatement("SELECT a.name area_name, s.name sector_name, p.id, p.name, p.rock, p.grade, p.locked_admin, p.locked_superadmin, MAX(m.id) media_id, MAX(m.checksum) media_crc32 FROM ((((((area a INNER JOIN region r ON a.region_id=r.id) INNER JOIN region_type rt ON r.id=rt.region_id) INNER JOIN sector s ON a.id=s.area_id) INNER JOIN problem p ON s.id=p.sector_id) LEFT JOIN user_region ur ON r.id=ur.region_id AND ur.user_id=?) LEFT JOIN media_problem mp ON p.id=mp.problem_id) LEFT JOIN media m ON mp.media_id=m.id AND m.is_movie=0 AND m.deleted_user_id IS NULL WHERE rt.type_id IN (SELECT type_id FROM region_type WHERE region_id=?) AND (r.id=? OR ur.user_id IS NOT NULL) AND (p.name LIKE ? OR p.name LIKE ? OR p.rock LIKE ? OR p.rock LIKE ?) AND is_readable(ur.admin_read, ur.superadmin_read, p.locked_admin, p.locked_superadmin, p.trash)=1 GROUP BY a.name, s.name, p.id, p.name, p.rock, p.grade, p.locked_admin, p.locked_superadmin, p.hits ORDER BY p.hits DESC, p.name, p.grade LIMIT 8")) {
			ps.setInt(1, authUserId);
			ps.setInt(2, setup.getIdRegion());
			ps.setInt(3, setup.getIdRegion());
			ps.setString(4, search + "%");
			ps.setString(5, "% " + search + "%");
			ps.setString(6, search + "%");
			ps.setString(7, "% " + search + "%");
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					String areaName = rst.getString("area_name");
					String sectorName = rst.getString("sector_name");
					int id = rst.getInt("id");
					String name = rst.getString("name");
					String rock = rst.getString("rock");
					int grade = rst.getInt("grade");
					boolean lockedAdmin = rst.getBoolean("locked_admin");
					boolean lockedSuperadmin = rst.getBoolean("locked_superadmin");
					int mediaId = rst.getInt("media_id");
					int mediaCrc32 = rst.getInt("media_crc32");
					problems.add(new Search(name + " [" + setup.getGradeConverter().getGradeFromIdGrade(grade) + "]", areaName + " / " + sectorName + (rock == null? "" : " (rock: " + rock + ")"), "/problem/" + id, null, null, mediaId, mediaCrc32, lockedAdmin, lockedSuperadmin));
				}
			}
		}
		// Users
		List<Search> users = new ArrayList<>();
		try (PreparedStatement ps = c.getConnection().prepareStatement("SELECT CASE WHEN picture IS NOT NULL THEN CONCAT('https://buldreinfo.com/buldreinfo_media/users/', id, '.jpg') END picture, id, TRIM(CONCAT(firstname, ' ', COALESCE(lastname,''))) name FROM user WHERE CONCAT(' ',firstname, ' ', COALESCE(lastname,'')) LIKE CONCAT('% ',?,'%') ORDER BY TRIM(CONCAT(firstname, ' ', COALESCE(lastname,''))) LIMIT 8")) {
			ps.setString(1, search + "%");
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					String picture = rst.getString("picture");
					int id = rst.getInt("id");
					String name = rst.getString("name");
					users.add(new Search(name, null, "/user/" + id, null, picture, 0, 0, false, false));
				}
			}
		}
		// Truncate result to max 9
		while (areas.size() + sectors.size() + problems.size() + users.size() > 10) {
			if (problems.size() > 5) {
				problems.remove(problems.size() - 1);
			} else if (areas.size() > 2) {
				areas.remove(areas.size() - 1);
			} else if (sectors.size() > 2) {
				sectors.remove(sectors.size() - 1);
			} else if (users.size() > 1) {
				users.remove(users.size() - 1);
			}
		}
		if (!areas.isEmpty()) {
			res.addAll(areas);
		}
		if (!sectors.isEmpty()) {
			res.addAll(sectors);
		}
		if (!problems.isEmpty()) {
			res.addAll(problems);
		}
		if (!users.isEmpty()) {
			res.addAll(users);
		}
		if (!externalAreas.isEmpty()) {
			res.addAll(externalAreas);
		}
		return res;
	}

	public Sector getSector(int authUserId, boolean orderByGrade, Setup setup, int reqId) throws IOException, SQLException {
		final boolean updateHits = true;
		return getSector(authUserId, orderByGrade, setup, reqId, updateHits);
	}

	public List<Setup> getSetups() throws SQLException {
		List<Setup> res = new ArrayList<>();
		try (PreparedStatement ps = c.getConnection().prepareStatement("SELECT r.id id_region, r.title, r.description, REPLACE(REPLACE(r.url,'https://',''),'http://','') domain, r.latitude, r.longitude, r.default_zoom, t.group FROM region r, region_type rt, type t WHERE r.id=rt.region_id AND rt.type_id=t.id GROUP BY r.id, r.title, r.description, r.url, r.latitude, r.longitude, r.default_zoom, t.group ORDER BY r.id")) {
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					int idRegion = rst.getInt("id_region");
					String title = rst.getString("title");
					String description = rst.getString("description");
					String domain = rst.getString("domain");
					double latitude = rst.getDouble("latitude");
					double longitude = rst.getDouble("longitude");
					int defaultZoom = rst.getInt("default_zoom");
					GRADE_SYSTEM gradeSystem = null;
					String group = rst.getString("group");
					switch (group) {
					case "Bouldering": gradeSystem = GRADE_SYSTEM.BOULDER; break;
					case "Climbing": gradeSystem = GRADE_SYSTEM.CLIMBING; break;
					case "Ice": gradeSystem = GRADE_SYSTEM.ICE; break;
					default: throw new RuntimeException("Invalid group: " + group);
					}
					List<CompassDirection> compassDirections = getCompassDirections();
					GradeConverter gradeConverter = new GradeConverter(getGrades(gradeSystem));
					res.add(new Setup(domain, gradeSystem)
							.setIdRegion(idRegion)
							.setTitle(title)
							.setDescription(description)
							.setLatLng(latitude, longitude)
							.setDefaultZoom(defaultZoom)
							.setCompassDirections(compassDirections)
							.setGradeConverter(gradeConverter));
				}
			}
		}
		logger.debug("getSetups() - res.size()={}", res.size());
		return res;
	}

	public String getSitemapTxt(Setup setup) throws SQLException {
		List<String> urls = new ArrayList<>();
		// Fixed urls
		urls.add(setup.getUrl(null));
		urls.add(setup.getUrl("/about"));
		urls.add(setup.getUrl("/areas"));
		urls.add(setup.getUrl("/filter"));
		urls.add(setup.getUrl("/gpl-3.0.txt"));
		urls.add(setup.getUrl("/problems"));
		urls.add(setup.getUrl("/sites/bouldering"));
		urls.add(setup.getUrl("/sites/climbing"));
		urls.add(setup.getUrl("/sites/ice"));
		// Users
		try (PreparedStatement ps = c.getConnection().prepareStatement("SELECT f.user_id FROM area a, sector s, problem p, fa f WHERE a.region_id=? AND a.locked_admin=0 AND a.locked_superadmin=0 AND a.id=s.area_id AND s.locked_admin=0 AND s.locked_superadmin=0 AND s.id=p.sector_id AND p.locked_admin=0 AND p.locked_superadmin=0 AND p.id=f.problem_id GROUP BY f.user_id UNION SELECT t.user_id FROM area a, sector s, problem p, tick t WHERE a.region_id=? AND a.locked_admin=0 AND a.locked_superadmin=0 AND a.id=s.area_id AND s.locked_admin=0 AND s.locked_superadmin=0 AND s.id=p.sector_id AND p.locked_admin=0 AND p.locked_superadmin=0 AND p.id=t.problem_id GROUP BY t.user_id")) {
			ps.setInt(1, setup.getIdRegion());
			ps.setInt(2, setup.getIdRegion());
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					int userId = rst.getInt("user_id");
					urls.add(setup.getUrl("/user/" + userId));
				}
			}
		}
		// Areas, sectors, problems
		try (PreparedStatement ps = c.getConnection().prepareStatement("SELECT CONCAT('/area/', a.id) url FROM region r, area a WHERE r.id=? AND r.id=a.region_id AND a.locked_admin=0 AND a.locked_superadmin=0 UNION SELECT CONCAT('/sector/', s.id) url FROM region r, area a, sector s WHERE r.id=? AND r.id=a.region_id AND a.locked_admin=0 AND a.locked_superadmin=0 AND a.id=s.area_id AND s.locked_admin=0 AND s.locked_superadmin=0 UNION SELECT CONCAT('/problem/', p.id) url FROM region r, area a, sector s, problem p WHERE r.id=? AND r.id=a.region_id AND a.locked_admin=0 AND a.locked_superadmin=0 AND a.id=s.area_id AND s.locked_admin=0 AND s.locked_superadmin=0 AND s.id=p.sector_id AND p.locked_admin=0 AND p.locked_superadmin=0")) {
			ps.setInt(1, setup.getIdRegion());
			ps.setInt(2, setup.getIdRegion());
			ps.setInt(3, setup.getIdRegion());
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					urls.add(setup.getUrl(rst.getString("url")));
				}
			}
		}
		return Joiner.on("\r\n").join(urls);
	}

	public List<Site> getSites(int currIdRegion) throws SQLException {
		Map<Integer, Site> regionLookup = new LinkedHashMap<>();
		try (PreparedStatement ps = c.getConnection().prepareStatement("SELECT r.id region_id, t.group, r.name, r.url FROM region r, region_type rt, type t WHERE r.id=rt.region_id AND rt.type_id=t.id AND t.id IN (1,2,10) GROUP BY r.id, t.group, r.name, r.url ORDER BY t.group, r.name")) {
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					int idRegion = rst.getInt("region_id");
					String group = rst.getString("group");
					String name = rst.getString("name");
					String url = rst.getString("url");
					boolean active = idRegion == currIdRegion;
					regionLookup.put(idRegion, new Site(group, name, url, active));
				}
			}
		}
		if (!regionLookup.isEmpty()) {
			// Fill region outlines
			Multimap<Integer, Coordinates> idRegionOutline = getRegionOutlines(regionLookup.keySet());
			for (int idRegion : idRegionOutline.keySet()) {
				List<Coordinates> outline = Lists.newArrayList(idRegionOutline.get(idRegion));
				regionLookup.get(idRegion).setOutline(outline);
			}
		}
		return Lists.newArrayList(regionLookup.values());
	}

	public Ticks getTicks(int authUserId, Setup setup, int page) throws SQLException {
		final int take = 200;
		int numTicks = 0;
		int skip = (page-1)*take;
		String sqlStr = "SELECT a.name area_name, a.locked_admin area_locked_admin, a.locked_superadmin area_locked_superadmin, s.name sector_name, s.locked_admin sector_locked_admin, s.locked_superadmin sector_locked_superadmin, p.id problem_id, t.grade problem_grade, p.name problem_name, p.locked_admin problem_locked_admin, p.locked_superadmin problem_locked_superadmin, DATE_FORMAT(t.date,'%Y.%m.%d') ts, TRIM(CONCAT(u.firstname, ' ', IFNULL(u.lastname,''))) name"
				+ " FROM ((((((region r INNER JOIN region_type rt ON r.id=rt.region_id) INNER JOIN area a ON r.id=a.region_id) INNER JOIN sector s ON a.id=s.area_id) INNER JOIN problem p ON s.id=p.sector_id) INNER JOIN tick t ON p.id=t.problem_id) INNER JOIN user u ON t.user_id=u.id) LEFT JOIN user_region ur ON r.id=ur.region_id AND ur.user_id=?"
				+ "  WHERE rt.type_id IN (SELECT type_id FROM region_type WHERE region_id=?)"
				+ "    AND (r.id=? OR ur.user_id IS NOT NULL)"
				+ "    AND is_readable(ur.admin_read, ur.superadmin_read, a.locked_admin, a.locked_superadmin, a.trash)=1"
				+ "    AND is_readable(ur.admin_read, ur.superadmin_read, s.locked_admin, s.locked_superadmin, s.trash)=1"
				+ "    AND is_readable(ur.admin_read, ur.superadmin_read, p.locked_admin, p.locked_superadmin, p.trash)=1"
				+ " GROUP BY a.name, a.locked_admin, a.locked_superadmin, s.name, s.locked_admin, s.locked_superadmin, p.id, t.grade, p.name, p.locked_admin, p.locked_superadmin, t.date, u.firstname, u.lastname"
				+ " ORDER BY t.date DESC, problem_name, name";
		List<PublicAscent> ticks = new ArrayList<>();
		try (PreparedStatement ps = c.getConnection().prepareStatement(sqlStr)) {
			ps.setInt(1, authUserId);
			ps.setInt(2, setup.getIdRegion());
			ps.setInt(3, setup.getIdRegion());
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					numTicks++;
					if ((numTicks-1) < skip || ticks.size() == take) {
						continue;
					}
					String areaName = rst.getString("area_name");
					boolean areaLockedAdmin = rst.getBoolean("area_locked_admin"); 
					boolean areaLockedSuperadmin = rst.getBoolean("area_locked_superadmin");
					String sectorName = rst.getString("sector_name");
					boolean sectorLockedAdmin = rst.getBoolean("sector_locked_admin"); 
					boolean sectorLockedSuperadmin = rst.getBoolean("sector_locked_superadmin");
					int problemId = rst.getInt("problem_id");
					int problemGrade = rst.getInt("problem_grade");
					String problemName = rst.getString("problem_name");
					boolean problemLockedAdmin = rst.getBoolean("problem_locked_admin");
					boolean problemLockedSuperadmin = rst.getBoolean("problem_locked_superadmin");
					String date = rst.getString("ts");
					String name = rst.getString("name");
					ticks.add(new PublicAscent(areaName, areaLockedAdmin, areaLockedSuperadmin, sectorName, sectorLockedAdmin, sectorLockedSuperadmin, problemId, setup.getGradeConverter().getGradeFromIdGrade(problemGrade), problemName, problemLockedAdmin, problemLockedSuperadmin, date, name));
				}
			}
		}
		int numPages = (int)(Math.ceil(numTicks / 200f));
		Ticks res = new Ticks(ticks, page, numPages);
		logger.debug("getTicks(authUserId={}, idRegion={}, page={}) - res={}", authUserId, setup.getIdRegion(), page, res);
		return res;
	}

	public Todo getTodo(int authUserId, Setup setup, int idArea, int idSector) throws SQLException {
		Todo res = new Todo();
		Map<Integer, Todo.TodoSector> sectorLookup = new HashMap<>();
		Map<Integer, Todo.TodoProblem> problemLookup = new HashMap<>();
		String condition = null;
		int id = 0;
		if (idSector > 0) {
			condition = "s.id=?";
			id = idSector;
		}
		else if (idArea > 0) {
			condition = "a.id=?";
			id = idArea;
		}
		else {
			throw new RuntimeException("Invalid arguments");
		}
		String sqlStr = "SELECT s.id sector_id, s.name sector_name, s.locked_admin sector_locked_admin, s.locked_superadmin sector_locked_superadmin, t.id todo_id, p.id problem_id, p.nr problem_nr, p.name problem_name, p.grade problem_grade, p.locked_admin problem_locked_admin, p.locked_superadmin problem_locked_superadmin,"
				+ " u.id user_id, TRIM(CONCAT(u.firstname, ' ', COALESCE(u.lastname,''))) user_name"
				+ " FROM (((((region r INNER JOIN area a ON r.id=a.region_id) INNER JOIN sector s ON a.id=s.area_id) INNER JOIN problem p ON s.id=p.sector_id) INNER JOIN todo t ON p.id=t.problem_id) INNER JOIN user u ON t.user_id=u.id) LEFT JOIN user_region ur ON (r.id=ur.region_id AND ur.user_id=?)"
				+ " WHERE " + condition
				+ " AND is_readable(ur.admin_read, ur.superadmin_read, a.locked_admin, a.locked_superadmin, a.trash)=1"
				+ " AND is_readable(ur.admin_read, ur.superadmin_read, s.locked_admin, s.locked_superadmin, s.trash)=1"
				+ " AND is_readable(ur.admin_read, ur.superadmin_read, p.locked_admin, p.locked_superadmin, p.trash)=1"
				+ " ORDER BY a.name, s.sorting, s.name, p.nr, u.firstname, u.lastname";
		try (PreparedStatement ps = c.getConnection().prepareStatement(sqlStr)) {
			ps.setInt(1, authUserId);
			ps.setInt(2, id);
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					// Sector
					int sectorId = rst.getInt("sector_id");
					Todo.TodoSector s = sectorLookup.get(sectorId);
					if (s == null) {
						String sectorName = rst.getString("sector_name");
						boolean sectorLockedAdmin = rst.getBoolean("sector_locked_admin"); 
						boolean sectorLockedSuperadmin = rst.getBoolean("sector_locked_superadmin");
						s = res.addSector(sectorId, sectorName, sectorLockedAdmin, sectorLockedSuperadmin);
						sectorLookup.put(sectorId, s);
					}
					// Problem
					int problemId = rst.getInt("problem_id");
					Todo.TodoProblem p = problemLookup.get(problemId);
					if (p == null) {
						int problemNr = rst.getInt("problem_nr");
						String problemName = rst.getString("problem_name");
						int problemGrade = rst.getInt("problem_grade");
						boolean problemLockedAdmin = rst.getBoolean("problem_locked_admin");
						boolean problemLockedSuperadmin = rst.getBoolean("problem_locked_superadmin");
						p = s.addProblem(problemId, problemLockedAdmin, problemLockedSuperadmin, problemNr, problemName, setup.getGradeConverter().getGradeFromIdGrade(problemGrade));
						problemLookup.put(problemId, p);
					}
					// Partner
					int userId = rst.getInt("user_id");
					String userName = rst.getString("user_name");
					p.addPartner(userId, userName);
				}
			}
		}
		logger.debug("getTodo(authUserId={}, idArea={}, idSector)={}) - res={}", authUserId, setup.getIdRegion(), idArea, idSector, res);
		return res;
	}

	public List<Top> getTop(int authUserId, Setup s, int areaId, int sectorId) throws SQLException {
		List<Top> res = new ArrayList<>();
		String condition = (sectorId>0? "s.id=" + sectorId : "a.id=" + areaId);
		String sqlStr = "WITH x AS ("
				+ "  SELECT COUNT(p.id) sum"
				+ "  FROM area a, sector s, problem p"
				+ "  WHERE " + condition 
				+ "    AND a.id=s.area_id AND s.id=p.sector_id AND p.grade!=0 AND p.broken IS NULL)"
				+ " SELECT y.user_id, y.name, y.picture, ROUND(SUM(y.sum)/x.sum*100,2) percentage"
				+ " FROM ("
				+ "  SELECT 'tick' t, u.id user_id, TRIM(CONCAT(u.firstname, ' ', COALESCE(u.lastname,''))) name, CASE WHEN u.picture IS NOT NULL THEN CONCAT('https://buldreinfo.com/buldreinfo_media/users/', u.id, '.jpg') END picture, COUNT(p.id) sum"
				+ "  FROM area a, sector s, problem p, tick t, user u"
				+ "  WHERE " + condition
				+ "    AND a.id=s.area_id AND s.id=p.sector_id AND p.id=t.problem_id AND t.user_id=u.id"
				+ "  GROUP BY u.id, u.firstname, u.lastname, u.picture"
				+ "  UNION"
				+ "  SELECT 'fa' t, u.id user_id, TRIM(CONCAT(u.firstname, ' ', COALESCE(u.lastname,''))) name, CASE WHEN u.picture IS NOT NULL THEN CONCAT('https://buldreinfo.com/buldreinfo_media/users/', u.id, '.jpg') END picture, COUNT(p.id) sum"
				+ "  FROM area a, sector s, problem p, fa f, user u"
				+ "  WHERE " + condition
				+ "    AND a.id=s.area_id AND s.id=p.sector_id AND p.id=f.problem_id AND f.user_id=u.id"
				+ "    AND (p.id, u.id) NOT IN (SELECT problem_id, user_id FROM tick)"
				+ "  GROUP BY u.id, u.firstname, u.lastname, u.picture"
				+ "  UNION"
				+ "  SELECT 'fa_aid' t, u.id user_id, TRIM(CONCAT(u.firstname, ' ', COALESCE(u.lastname,''))) name, CASE WHEN u.picture IS NOT NULL THEN CONCAT('https://buldreinfo.com/buldreinfo_media/users/', u.id, '.jpg') END picture, COUNT(p.id) sum"
				+ "  FROM area a, sector s, problem p, fa_aid_user f, user u"
				+ "  WHERE " + condition
				+ "    AND a.id=s.area_id AND s.id=p.sector_id AND p.id=f.problem_id AND f.user_id=u.id"
				+ "    AND (p.id, u.id) NOT IN (SELECT problem_id, user_id FROM tick)"
				+ "  GROUP BY u.id, u.firstname, u.lastname, u.picture"
				+ ") y, x"
				+ " GROUP BY y.user_id, y.name, y.picture, x.sum"
				+ " ORDER BY percentage DESC, name";
		try (PreparedStatement ps = c.getConnection().prepareStatement(sqlStr)) {
			try (ResultSet rst = ps.executeQuery()) {
				double prevPercentage = 0;
				int prevRank = 0;
				while (rst.next()) {
					int userId = rst.getInt("user_id");
					String name = rst.getString("name");
					String picture = rst.getString("picture");
					double percentage = rst.getDouble("percentage");
					int rank = res.size()+1;
					if (prevPercentage == percentage) {
						rank = prevRank;
					}
					prevPercentage = percentage;
					prevRank = rank;
					boolean mine = authUserId == userId;
					res.add(new Top(rank, userId, name, picture, percentage, mine));
				}
			}
		}
		return res;
	}

	public List<Trash> getTrash(int authUserId, Setup setup) throws IOException, SQLException {
		ensureAdminWriteRegion(authUserId, setup.getIdRegion());
		List<Trash> res = new ArrayList<>();
		String sqlStr =
				// Area
				"SELECT a.id area_id, null sector_id, null problem_id, null media_id, a.name, DATE_FORMAT(a.trash,'%Y.%m.%d-%k:%i:%s') trash, TRIM(CONCAT(u.firstname, ' ', COALESCE(u.lastname,''))) trash_by"
				+ " FROM (((region r INNER JOIN region_type rt ON r.id=rt.region_id) INNER JOIN area a ON r.id=a.region_id) INNER JOIN user u ON a.trash_by=u.id) LEFT JOIN user_region ur ON (r.id=ur.region_id AND ur.user_id=?)"
				+ " WHERE a.trash IS NOT NULL AND rt.type_id IN (SELECT type_id FROM region_type WHERE region_id=?) AND (r.id=? OR ur.user_id IS NOT NULL) AND is_readable(ur.admin_read, ur.superadmin_read, a.locked_admin, a.locked_superadmin, null)=1"
				+ " GROUP BY area_id, sector_id, problem_id, media_id, name, trash, trash_by"
				// Sector
				+ " UNION ALL"
				+ " SELECT null area_id, s.id sector_id, null problem_id, null media_id, CONCAT(s.name,' (',a.name,')') name, DATE_FORMAT(s.trash,'%Y.%m.%d-%k:%i:%s') trash, TRIM(CONCAT(u.firstname, ' ', COALESCE(u.lastname,''))) trash_by"
				+ " FROM ((((region r INNER JOIN region_type rt ON r.id=rt.region_id) INNER JOIN area a ON r.id=a.region_id) INNER JOIN sector s ON a.id=s.area_id) INNER JOIN user u ON s.trash_by=u.id) LEFT JOIN user_region ur ON (r.id=ur.region_id AND ur.user_id=?)"
				+ " WHERE a.trash IS NULL AND s.trash IS NOT NULL AND rt.type_id IN (SELECT type_id FROM region_type WHERE region_id=?) AND (r.id=? OR ur.user_id IS NOT NULL) AND is_readable(ur.admin_read, ur.superadmin_read, s.locked_admin, s.locked_superadmin, null)=1"
				+ " GROUP BY area_id, sector_id, problem_id, media_id, name, trash, trash_by"
				// Problem
				+ " UNION ALL"
				+ " SELECT null area_id, null sector_id, p.id problem_id, null media_id, CONCAT(p.name,' (',a.name,'/',s.name,')') name, DATE_FORMAT(p.trash,'%Y.%m.%d-%k:%i:%s') trash, TRIM(CONCAT(u.firstname, ' ', COALESCE(u.lastname,''))) trash_by"
				+ " FROM (((((region r INNER JOIN region_type rt ON r.id=rt.region_id) INNER JOIN area a ON r.id=a.region_id) INNER JOIN sector s ON a.id=s.area_id) INNER JOIN problem p ON s.id=p.sector_id) INNER JOIN user u ON p.trash_by=u.id) LEFT JOIN user_region ur ON (r.id=ur.region_id AND ur.user_id=?)"
				+ " WHERE a.trash IS NULL AND s.trash IS NULL AND p.trash IS NOT NULL AND rt.type_id IN (SELECT type_id FROM region_type WHERE region_id=?) AND (r.id=? OR ur.user_id IS NOT NULL) AND is_readable(ur.admin_read, ur.superadmin_read, p.locked_admin, p.locked_superadmin, null)=1"
				+ " GROUP BY area_id, sector_id, problem_id, media_id, name, trash, trash_by"
				// Media (Area)
				+ " UNION ALL"
				+ " SELECT a.id area_id, null sector_id, null problem_id, m.id media_id, a.name, DATE_FORMAT(m.deleted_timestamp,'%Y.%m.%d-%k:%i:%s') trash, TRIM(CONCAT(u.firstname, ' ', COALESCE(u.lastname,''))) trash_by"
				+ " FROM (((((region r INNER JOIN region_type rt ON r.id=rt.region_id) INNER JOIN area a ON r.id=a.region_id) INNER JOIN media_area ma ON a.id=ma.area_id) INNER JOIN media m ON ma.media_id=m.id) INNER JOIN user u ON m.deleted_user_id=u.id) LEFT JOIN user_region ur ON (r.id=ur.region_id AND ur.user_id=?)"
				+ " WHERE m.deleted_user_id IS NOT NULL AND rt.type_id IN (SELECT type_id FROM region_type WHERE region_id=?) AND (r.id=? OR ur.user_id IS NOT NULL) AND is_readable(ur.admin_read, ur.superadmin_read, a.locked_admin, a.locked_superadmin, null)=1"
				+ " GROUP BY area_id, sector_id, problem_id, media_id, name, trash, trash_by"
				// Media (Sector)
				+ " UNION ALL"
				+ " SELECT null area_id, s.id sector_id, null problem_id, m.id media_id, CONCAT(s.name,' (',a.name,')') name, DATE_FORMAT(m.deleted_timestamp,'%Y.%m.%d-%k:%i:%s') trash, TRIM(CONCAT(u.firstname, ' ', COALESCE(u.lastname,''))) trash_by"
				+ " FROM ((((((region r INNER JOIN region_type rt ON r.id=rt.region_id) INNER JOIN area a ON r.id=a.region_id) INNER JOIN sector s ON a.id=s.area_id) INNER JOIN media_sector ms ON s.id=ms.sector_id) INNER JOIN media m ON ms.media_id=m.id) INNER JOIN user u ON m.deleted_user_id=u.id) LEFT JOIN user_region ur ON (r.id=ur.region_id AND ur.user_id=?)"
				+ " WHERE m.deleted_user_id IS NOT NULL AND rt.type_id IN (SELECT type_id FROM region_type WHERE region_id=?) AND (r.id=? OR ur.user_id IS NOT NULL) AND is_readable(ur.admin_read, ur.superadmin_read, a.locked_admin, a.locked_superadmin, null)=1"
				+ " GROUP BY area_id, sector_id, problem_id, media_id, name, trash, trash_by"
				// Media (Problem)
				+ " UNION ALL"
				+ " SELECT null area_id, null sector_id, p.id problem_id, m.id media_id, CONCAT(p.name,' (',a.name,'/',s.name,')') name, DATE_FORMAT(m.deleted_timestamp,'%Y.%m.%d-%k:%i:%s') trash, TRIM(CONCAT(u.firstname, ' ', COALESCE(u.lastname,''))) trash_by"
				+ " FROM (((((((region r INNER JOIN region_type rt ON r.id=rt.region_id) INNER JOIN area a ON r.id=a.region_id) INNER JOIN sector s ON a.id=s.area_id) INNER JOIN problem p ON s.id=p.sector_id) INNER JOIN media_problem mp ON p.id=mp.problem_id) INNER JOIN media m ON mp.media_id=m.id) INNER JOIN user u ON m.deleted_user_id=u.id) LEFT JOIN user_region ur ON (r.id=ur.region_id AND ur.user_id=?)"
				+ " WHERE m.deleted_user_id IS NOT NULL AND rt.type_id IN (SELECT type_id FROM region_type WHERE region_id=?) AND (r.id=? OR ur.user_id IS NOT NULL) AND is_readable(ur.admin_read, ur.superadmin_read, a.locked_admin, a.locked_superadmin, null)=1"
				+ " GROUP BY area_id, sector_id, problem_id, media_id, name, trash, trash_by"
				// Order results
				+ " ORDER BY trash DESC";
		try (PreparedStatement ps = c.getConnection().prepareStatement(sqlStr)) {
			ps.setInt(1, authUserId);
			ps.setInt(2, setup.getIdRegion());
			ps.setInt(3, setup.getIdRegion());
			ps.setInt(4, authUserId);
			ps.setInt(5, setup.getIdRegion());
			ps.setInt(6, setup.getIdRegion());
			ps.setInt(7, authUserId);
			ps.setInt(8, setup.getIdRegion());
			ps.setInt(9, setup.getIdRegion());
			ps.setInt(10, authUserId);
			ps.setInt(11, setup.getIdRegion());
			ps.setInt(12, setup.getIdRegion());
			ps.setInt(13, authUserId);
			ps.setInt(14, setup.getIdRegion());
			ps.setInt(15, setup.getIdRegion());
			ps.setInt(16, authUserId);
			ps.setInt(17, setup.getIdRegion());
			ps.setInt(18, setup.getIdRegion());
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					int areaId = rst.getInt("area_id");
					int sectorId = rst.getInt("sector_id");
					int problemId = rst.getInt("problem_id");
					int mediaId = rst.getInt("media_id");
					String name = rst.getString("name");
					String when = rst.getString("trash");
					String by = rst.getString("trash_by");
					res.add(new Trash(areaId, sectorId, problemId, mediaId, name, when, by));
				}
			}
		}
		return res;
	}

	public List<Type> getTypes(int regionId) throws SQLException {
		List<Type> res = new ArrayList<>();
		try (PreparedStatement ps = c.getConnection().prepareStatement("SELECT t.id, t.type, t.subtype FROM type t, region_type rt WHERE t.id=rt.type_id AND rt.region_id=? GROUP BY t.id, t.type, t.subtype ORDER BY t.id, t.type, t.subtype")) {
			ps.setInt(1, regionId);
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					int id = rst.getInt("id");
					String type = rst.getString("type");
					String subtype = rst.getString("subtype");
					res.add(new Type(id, type, subtype));
				}
			}
		}
		return res;
	}

	public List<UserRegion> getUserRegion(int authUserId, Setup setup) throws SQLException {
		List<UserRegion> res = new ArrayList<>();
		try (PreparedStatement ps = c.getConnection().prepareStatement("SELECT r.id, r.name, CASE WHEN r.id=? OR ur.admin_read=1 OR ur.admin_write=1 OR ur.superadmin_read=1 OR ur.superadmin_write=1 THEN 1 ELSE 0 END read_only, ur.region_visible, CASE WHEN ur.superadmin_write=1 THEN 'Superadmin' WHEN ur.superadmin_read=1 THEN 'Superadmin (read)' WHEN ur.admin_read=1 THEN 'Admin (read)' WHEN ur.admin_write=1 THEN 'Admin' END role FROM (region r INNER JOIN region_type rt ON r.id=rt.region_id) LEFT JOIN user_region ur ON r.id=ur.region_id AND ur.user_id=? WHERE rt.type_id IN (SELECT type_id FROM region_type WHERE region_id=?) GROUP BY r.id, r.name ORDER BY r.name")) {
			ps.setInt(1, setup.getIdRegion());
			ps.setInt(2, authUserId);
			ps.setInt(3, setup.getIdRegion());
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					int id = rst.getInt("id");
					String name = rst.getString("name");
					String role = rst.getString("role");
					boolean readOnly = rst.getBoolean("read_only");
					boolean enabled = readOnly || rst.getBoolean("region_visible");
					res.add(new UserRegion(id, name, role, enabled, readOnly));
				}
			}
		}
		return res;
	}

	public List<UserSearch> getUserSearch(int authUserId, String value) throws SQLException {
		if (authUserId == -1) {
			throw new SQLException("User not logged in...");
		}
		List<UserSearch> res = new ArrayList<>();
		try (PreparedStatement ps = c.getConnection().prepareStatement("SELECT id, TRIM(CONCAT(firstname, ' ', COALESCE(lastname,''))) name FROM user WHERE CONCAT(' ',firstname, ' ', COALESCE(lastname,'')) LIKE CONCAT(' %',?,'%') ORDER BY firstname, lastname")) {
			ps.setString(1, value);
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					int id = rst.getInt("id");
					String name = rst.getString("name");
					res.add(new UserSearch(id, name));
				}
			}
		}
		return res;
	}
	
	public byte[] getUserTicks(int authUserId) throws SQLException, IOException {
		byte[] bytes;
		try (ExcelReport report = new ExcelReport()) {
			String sqlStr = "SELECT r.id region_id, ty.type, pt.subtype, COUNT(DISTINCT ps.id) num_pitches, CONCAT(r.url,'/problem/',p.id) url, a.name area_name, s.name sector_name, p.name, CASE WHEN (t.id IS NOT NULL) THEN t.comment ELSE p.description END comment, DATE_FORMAT(CASE WHEN t.date IS NULL AND f.user_id IS NOT NULL THEN p.fa_date ELSE t.date END,'%Y-%m-%d') date, t.stars, CASE WHEN (f.user_id IS NOT NULL) THEN f.user_id ELSE 0 END fa, (CASE WHEN t.id IS NOT NULL THEN t.grade ELSE p.grade END) grade" + 
					" FROM (((((((((problem p INNER JOIN type pt ON p.type_id=pt.id) INNER JOIN sector s ON p.sector_id=s.id) INNER JOIN area a ON s.area_id=a.id) INNER JOIN region r ON a.region_id=r.id) INNER JOIN region_type rt ON r.id=rt.region_id) INNER JOIN type ty ON rt.type_id=ty.id) LEFT JOIN problem_section ps ON p.id=ps.problem_id) LEFT JOIN user_region ur ON (r.id=ur.region_id AND ur.user_id=?)) LEFT JOIN tick t ON p.id=t.problem_id AND t.user_id=?) LEFT JOIN fa f ON (p.id=f.problem_id AND f.user_id=?)" + 
					" WHERE (t.user_id IS NOT NULL OR f.user_id IS NOT NULL) AND is_readable(ur.admin_read, ur.superadmin_read, p.locked_admin, p.locked_superadmin, p.trash)=1" + 
					" GROUP BY r.id, ty.type, pt.subtype, r.url, a.name, a.locked_admin, a.locked_superadmin, s.name, s.locked_admin, s.locked_superadmin, t.id, p.id, p.locked_admin, p.locked_superadmin, p.name, p.description, p.fa_date, t.date, t.stars, t.grade, p.grade" + 
					" ORDER BY ty.type, a.name, s.name, p.name";
			try (PreparedStatement ps = c.getConnection().prepareStatement(sqlStr)) {
				ps.setInt(1, authUserId);
				ps.setInt(2, authUserId);
				ps.setInt(3, authUserId);
				try (ResultSet rst = ps.executeQuery()) {
					Map<String, SheetWriter> writers = new HashMap<>();
					while (rst.next()) {
						int regionId = rst.getInt("region_id");
						String type = rst.getString("type");
						String subType = rst.getString("subtype");
						int numPitches = rst.getInt("num_pitches");
						String url = rst.getString("url");
						String areaName = rst.getString("area_name");
						String sectorName = rst.getString("sector_name");
						String name = rst.getString("name");
						String comment = rst.getString("comment");
						Date date = rst.getDate("date");
						int stars = rst.getInt("stars");
						boolean fa = rst.getBoolean("fa");
						Setup setup = MetaHelper.getMeta().getSetup(regionId);
						String grade = setup.getGradeConverter().getGradeFromIdGrade(rst.getInt("grade"));
						SheetWriter writer = writers.get(type);
						if (writer == null) {
							writer = report.addSheet(type);
							writers.put(type, writer);
						}
						writer.incrementRow();
						writer.write("AREA", areaName);
						writer.write("SECTOR", sectorName);
						if (subType != null) {
							writer.write("TYPE", subType);
							writer.write("PITCHES", numPitches > 0? numPitches : 1);
						}
						writer.write("NAME", name);
						writer.write("FIRST ASCENT", fa? "Yes" : "No");
						writer.write("DATE", date);
						writer.write("GRADE", grade);
						writer.write("STARS", stars);
						writer.write("DESCRIPTION", comment);
						writer.write("URL", SheetHyperlink.of(url));
					}
					for (SheetWriter writer : writers.values()) {
						writer.close();
					}
				}
				try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
					report.writeExcel(os);
					bytes = os.toByteArray();
				}
			}
			sqlStr = "SELECT r.id region_id, ty.type, pt.subtype, COUNT(DISTINCT ps.id) num_pitches, CONCAT(r.url,'/problem/',p.id) url, a.name area_name, s.name sector_name, p.name, tr.comment, DATE_FORMAT(tr.date,'%Y-%m-%d') date, t.stars, 0 fa, t.grade grade"
					+ " FROM (((((((((problem p INNER JOIN type pt ON p.type_id=pt.id) INNER JOIN sector s ON p.sector_id=s.id) INNER JOIN area a ON s.area_id=a.id) INNER JOIN region r ON a.region_id=r.id) INNER JOIN region_type rt ON r.id=rt.region_id) INNER JOIN type ty ON rt.type_id=ty.id) INNER JOIN tick t ON p.id=t.problem_id AND t.user_id=?) INNER JOIN tick_repeat tr ON t.id=tr.tick_id) LEFT JOIN problem_section ps ON p.id=ps.problem_id) LEFT JOIN user_region ur ON (r.id=ur.region_id AND ur.user_id=?)"
					+ " WHERE is_readable(ur.admin_read, ur.superadmin_read, p.locked_admin, p.locked_superadmin, p.trash)=1"
					+ " GROUP BY r.id, ty.type, pt.subtype, r.url, a.name, a.locked_admin, a.locked_superadmin, s.name, s.locked_admin, s.locked_superadmin, t.id, p.id, p.locked_admin, p.locked_superadmin, p.name, tr.comment, p.fa_date, tr.date, t.stars, t.grade, p.grade"
					+ " ORDER BY ty.type, a.name, s.name, p.name, tr.date";
			try (PreparedStatement ps = c.getConnection().prepareStatement(sqlStr)) {
				ps.setInt(1, authUserId);
				ps.setInt(2, authUserId);
				try (ResultSet rst = ps.executeQuery()) {
					Map<String, SheetWriter> writers = new HashMap<>();
					while (rst.next()) {
						int regionId = rst.getInt("region_id");
						String type = rst.getString("type") + " (repeats)";
						String subType = rst.getString("subtype");
						int numPitches = rst.getInt("num_pitches");
						String url = rst.getString("url");
						String areaName = rst.getString("area_name");
						String sectorName = rst.getString("sector_name");
						String name = rst.getString("name");
						String comment = rst.getString("comment");
						Date date = rst.getDate("date");
						int stars = rst.getInt("stars");
						boolean fa = rst.getBoolean("fa");
						Setup setup = MetaHelper.getMeta().getSetup(regionId);
						String grade = setup.getGradeConverter().getGradeFromIdGrade(rst.getInt("grade"));
						SheetWriter writer = writers.get(type);
						if (writer == null) {
							writer = report.addSheet(type);
							writers.put(type, writer);
						}
						writer.incrementRow();
						writer.write("AREA", areaName);
						writer.write("SECTOR", sectorName);
						if (subType != null) {
							writer.write("TYPE", subType);
							writer.write("PITCHES", numPitches > 0? numPitches : 1);
						}
						writer.write("NAME", name);
						writer.write("FIRST ASCENT", fa? "Yes" : "No");
						writer.write("DATE", date);
						writer.write("GRADE", grade);
						writer.write("STARS", stars);
						writer.write("DESCRIPTION", comment);
						writer.write("URL", SheetHyperlink.of(url));
					}
					for (SheetWriter writer : writers.values()) {
						writer.close();
					}
				}
				try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
					report.writeExcel(os);
					bytes = os.toByteArray();
				}
			}
			sqlStr = "SELECT r.id region_id, CONCAT(r.url,'/problem/',p.id) url, a.name area_name, s.name sector_name, p.name, aid.aid_description comment, DATE_FORMAT(aid.aid_date,'%Y-%m-%d') date" + 
					" FROM (((((((problem p INNER JOIN sector s ON p.sector_id=s.id) INNER JOIN area a ON s.area_id=a.id) INNER JOIN region r ON a.region_id=r.id) INNER JOIN region_type rt ON r.id=rt.region_id) INNER JOIN type ty ON rt.type_id=ty.id) INNER JOIN fa_aid aid ON p.id=aid.problem_id) INNER JOIN fa_aid_user aid_u ON (p.id=aid_u.problem_id AND aid_u.user_id=?) LEFT JOIN user_region ur ON (r.id=ur.region_id AND ur.user_id=?))" + 
					" WHERE is_readable(ur.admin_read, ur.superadmin_read, p.locked_admin, p.locked_superadmin, p.trash)=1" + 
					" GROUP BY r.id, ty.type, r.url, a.name, a.locked_admin, a.locked_superadmin, s.name, s.locked_admin, s.locked_superadmin, p.id, p.locked_admin, p.locked_superadmin, p.name, aid.aid_description, aid.aid_date" + 
					" ORDER BY ty.type, a.name, s.name, p.name";
			try (PreparedStatement ps = c.getConnection().prepareStatement(sqlStr);
					SheetWriter writer = report.addSheet("First_AID_Ascent")) {
				ps.setInt(1, authUserId);
				ps.setInt(2, authUserId);
				try (ResultSet rst = ps.executeQuery()) {
					while (rst.next()) {
						String url = rst.getString("url");
						String areaName = rst.getString("area_name");
						String sectorName = rst.getString("sector_name");
						String name = rst.getString("name");
						String comment = rst.getString("comment");
						Date date = rst.getDate("date");
						writer.incrementRow();
						writer.write("AREA", areaName);
						writer.write("SECTOR", sectorName);
						writer.write("NAME", name);
						writer.write("DATE", date);
						writer.write("DESCRIPTION", comment);
						writer.write("URL", SheetHyperlink.of(url));
					}
					writer.close();
				}
				try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
					report.writeExcel(os);
					bytes = os.toByteArray();
				}
			}
		}
		return bytes;
	}

	public void moveMedia(int authUserId, int id, boolean left, int toIdSector, int toIdProblem) throws SQLException {
		boolean ok = false;
		int areaId = 0;
		int sectorId = 0;
		int problemId = 0;
		try (PreparedStatement ps = c.getConnection().prepareStatement("SELECT ur.admin_write, ur.superadmin_write, ma.area_id, ms.sector_id, mp.problem_id FROM ((((area a INNER JOIN sector s ON a.id=s.area_id) INNER JOIN user_region ur ON (a.region_id=ur.region_id AND ur.user_id=?)) LEFT JOIN media_area ma ON (a.id=ma.area_id AND ma.media_id=?) LEFT JOIN media_sector ms ON (s.id=ms.sector_id AND ms.media_id=?)) LEFT JOIN problem p ON s.id=p.sector_id) LEFT JOIN media_problem mp ON (p.id=mp.problem_id AND mp.media_id=?) WHERE ma.media_id IS NOT NULL OR ms.media_id IS NOT NULL OR mp.media_id IS NOT NULL GROUP BY ur.admin_write, ur.superadmin_write, ma.area_id, ms.sector_id, mp.problem_id")) {
			ps.setInt(1, authUserId);
			ps.setInt(2, id);
			ps.setInt(3, id);
			ps.setInt(4, id);
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					ok = rst.getBoolean("admin_write") || rst.getBoolean("superadmin_write");
					areaId = rst.getInt("area_id");
					sectorId = rst.getInt("sector_id");
					problemId = rst.getInt("problem_id");
				}
			}
		}
		Preconditions.checkArgument(ok, "Insufficient permissions");

		if (toIdSector > 0) {
			Preconditions.checkArgument(problemId>0);
			try (PreparedStatement ps = c.getConnection().prepareStatement("DELETE FROM media_problem WHERE media_id=? AND problem_id=?")) {
				ps.setInt(1, id);
				ps.setInt(2, problemId);
				ps.execute();
			}
			try (PreparedStatement ps = c.getConnection().prepareStatement("INSERT INTO media_sector (media_id, sector_id) VALUES (?, ?)")) {
				ps.setInt(1, id);
				ps.setInt(2, toIdSector);
				ps.execute();
			}
		}
		else if (toIdProblem > 0) {
			Preconditions.checkArgument(sectorId>0);
			try (PreparedStatement ps = c.getConnection().prepareStatement("DELETE FROM media_sector WHERE media_id=? AND sector_id=?")) {
				ps.setInt(1, id);
				ps.setInt(2, sectorId);
				ps.execute();
			}
			try (PreparedStatement ps = c.getConnection().prepareStatement("INSERT INTO media_problem (media_id, problem_id) VALUES (?, ?)")) {
				ps.setInt(1, id);
				ps.setInt(2, toIdProblem);
				ps.execute();
			}
		}
		else {
			String table = null;
			String column = null;
			String extraOrder = "";
			int columnId = 0;
			if (areaId > 0) {
				table = "media_area";
				column = "area_id";
				columnId = areaId;
			} else if (sectorId > 0) {
				table = "media_sector";
				column = "sector_id";
				columnId = sectorId;
			} else {
				table = "media_problem";
				column = "problem_id";
				columnId = problemId;
				extraOrder = "pitch, ";
			}
			List<Integer> idMediaList = new ArrayList<>();
			try (PreparedStatement ps = c.getConnection().prepareStatement("SELECT m.id FROM " + table + " x, media m WHERE x." + column + "=? AND x.media_id=m.id AND m.deleted_user_id IS NULL AND m.is_movie=0 ORDER BY " + extraOrder + " -x.sorting DESC, m.id")) {
				ps.setInt(1, columnId);
				try (ResultSet rst = ps.executeQuery()) {
					while (rst.next()) {
						int idMedia = rst.getInt("id");
						idMediaList.add(idMedia);
					}
				}
			}
			final int ixToMove = idMediaList.indexOf(id);
			idMediaList.remove(ixToMove);
			Preconditions.checkArgument(ixToMove>=0, "Could not find " + id + " in " + idMediaList);
			if (left) {
				if (ixToMove == 0) {
					idMediaList.add(id); // Move from start to end
				} else {
					idMediaList.add(ixToMove-1, id);
				}
			} else {
				if (ixToMove == idMediaList.size()) {
					idMediaList.add(0, id); // Move from end to start
				} else {
					idMediaList.add(ixToMove+1, id);
				}
			}
			try (PreparedStatement ps = c.getConnection().prepareStatement("UPDATE " + table + " SET sorting=? WHERE " + column + "=? AND media_id=?")) {
				int sorting = 0;
				for (int idMedia : idMediaList) {
					ps.setInt(1, ++sorting);
					ps.setInt(2, columnId);
					ps.setInt(3, idMedia);
					ps.addBatch();
				}
				ps.executeBatch();
			}
		}

		if (problemId > 0) {
			fillActivity(problemId);
		}
	}

	public void rotateMedia(int idRegion, int authUserId, int idMedia, int degrees) throws IOException, SQLException, InterruptedException {
		// Rotate allowed for administrators + user who uploaded specific image
		boolean uploadedByMe = getMedia(authUserId, idMedia).isUploadedByMe();
		if (!uploadedByMe) {
			ensureAdminWriteRegion(authUserId, idRegion);
		}
		GlobalFunctions.rotateMedia(c, idMedia, degrees);
	}

	public Redirect setArea(Setup s, int authUserId, Area a, FormDataMultiPart multiPart) throws NoSuchAlgorithmException, SQLException, IOException, InterruptedException {
		Preconditions.checkArgument(authUserId != -1, "Insufficient credentials");
		Preconditions.checkArgument(s.getIdRegion() > 0, "Insufficient credentials");
		ensureAdminWriteRegion(authUserId, s.getIdRegion());
		int idArea = -1;
		final boolean isLockedAdmin = a.isLockedSuperadmin()? false : a.isLockedAdmin();
		boolean setPermissionRecursive = false;
		if (a.getCoordinates() != null) {
			if (a.getCoordinates().getLatitude() == 0 || a.getCoordinates().getLongitude() == 0) {
				a.setCoordinates(null);
			}
			else {
				ensureCoordinatesInDbWithElevationAndId(Lists.newArrayList(a.getCoordinates()));
			}
		}
		if (a.getId() > 0) {
			ensureAdminWriteArea(authUserId, a.getId());
			Area currArea = getArea(s, authUserId, a.getId());
			setPermissionRecursive = currArea.isLockedAdmin() != isLockedAdmin || currArea.isLockedSuperadmin() != a.isLockedSuperadmin();
			try (PreparedStatement ps = c.getConnection().prepareStatement("UPDATE area SET name=?, description=?, coordinates_id=?, locked_admin=?, locked_superadmin=?, for_developers=?, access_info=?, access_closed=?, no_dogs_allowed=?, sun_from_hour=?, sun_to_hour=?, trash=CASE WHEN ? THEN NOW() ELSE NULL END, trash_by=? WHERE id=?")) {
				ps.setString(1, trimString(a.getName()));
				ps.setString(2, trimString(a.getComment()));
				setNullablePositiveInteger(ps, 3, a.getCoordinates() == null? 0 : a.getCoordinates().getId());
				ps.setBoolean(4, isLockedAdmin);
				ps.setBoolean(5, a.isLockedSuperadmin());
				ps.setBoolean(6, a.isForDevelopers());
				ps.setString(7, trimString(a.getAccessInfo()));
				ps.setString(8, trimString(a.getAccessClosed()));
				ps.setBoolean(9, a.isNoDogsAllowed());
				setNullablePositiveDouble(ps, 10, a.getSunFromHour());
				setNullablePositiveDouble(ps, 11, a.getSunToHour());
				ps.setBoolean(12, a.isTrash());
				ps.setInt(13, a.isTrash()? authUserId : 0);
				ps.setInt(14, a.getId());
				ps.execute();
			}
			idArea = a.getId();

			// Sector order
			if (a.getSectorOrder() != null) {
				for (AreaSectorOrder x : a.getSectorOrder()) {
					try (PreparedStatement ps = c.getConnection().prepareStatement("UPDATE sector SET sorting=? WHERE id=?")) {
						ps.setInt(1, x.getSorting());
						ps.setInt(2, x.getId());
						ps.execute();
					}
				}
			}

			// Also update sectors and problems (last_updated and locked)
			String sqlStr = null;
			if (setPermissionRecursive) {
				sqlStr = "UPDATE (area a LEFT JOIN sector s ON a.id=s.area_id) LEFT JOIN problem p ON s.id=p.sector_id SET a.last_updated=now(), a.locked_admin=?, a.locked_superadmin=?, s.last_updated=now(), s.locked_admin=?, s.locked_superadmin=?, p.last_updated=now(), p.locked_admin=?, p.locked_superadmin=? WHERE a.id=?";
				try (PreparedStatement ps = c.getConnection().prepareStatement(sqlStr)) {
					ps.setBoolean(1, isLockedAdmin);
					ps.setBoolean(2, a.isLockedSuperadmin());
					ps.setBoolean(3, isLockedAdmin);
					ps.setBoolean(4, a.isLockedSuperadmin());
					ps.setBoolean(5, isLockedAdmin);
					ps.setBoolean(6, a.isLockedSuperadmin());
					ps.setInt(7, a.getId());
					ps.execute();
				}
			} else {
				sqlStr = "UPDATE (area a LEFT JOIN sector s ON a.id=s.area_id) LEFT JOIN problem p ON s.id=p.sector_id SET a.last_updated=now(), s.last_updated=now(), p.last_updated=now() WHERE a.id=?";
				try (PreparedStatement ps = c.getConnection().prepareStatement(sqlStr)) {
					ps.setInt(1, idArea);
					ps.execute();
				}
			}
		} else {
			try (PreparedStatement ps = c.getConnection().prepareStatement("INSERT INTO area (android_id, region_id, name, description, coordinates_id, locked_admin, locked_superadmin, for_developers, access_info, access_closed, no_dogs_allowed, last_updated) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, now())", PreparedStatement.RETURN_GENERATED_KEYS)) {
				ps.setLong(1, System.currentTimeMillis());
				ps.setInt(2, s.getIdRegion());
				ps.setString(3, trimString(a.getName()));
				ps.setString(4, trimString(a.getComment()));
				setNullablePositiveInteger(ps, 5, a.getCoordinates() == null? 0 : a.getCoordinates().getId());
				ps.setBoolean(6, isLockedAdmin);
				ps.setBoolean(7, a.isLockedSuperadmin());
				ps.setBoolean(8, a.isForDevelopers());
				ps.setString(9, trimString(a.getAccessInfo()));
				ps.setString(10, trimString(a.getAccessClosed()));
				ps.setBoolean(11, a.isNoDogsAllowed());
				ps.executeUpdate();
				try (ResultSet rst = ps.getGeneratedKeys()) {
					if (rst != null && rst.next()) {
						idArea = rst.getInt(1);
					}
				}
			}
		}
		if (idArea == -1) {
			throw new SQLException("idArea == -1");
		}
		// New media
		if (a.getNewMedia() != null) {
			for (NewMedia m : a.getNewMedia()) {
				final int idProblem = 0;
				final int pitch = 0;
				final int idSector = 0;
				final int idGuestbook = 0;
				addNewMedia(authUserId, idProblem, pitch, m.isTrivia(), idSector, idArea, idGuestbook, m, multiPart);
			}
		}
		if (a.isTrash()) {
			return new Redirect(null, 0, 0, 0);
		}
		return new Redirect(null, idArea, 0, 0);
	}

	public Redirect setProblem(int authUserId, Setup s, Problem p, FormDataMultiPart multiPart) throws NoSuchAlgorithmException, SQLException, IOException, ParseException, InterruptedException {
		final boolean orderByGrade = s.isBouldering();
		final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		final Date dt = Strings.isNullOrEmpty(p.getFaDate()) ? null : new Date(sdf.parse(p.getFaDate()).getTime());
		int idProblem = -1;
		final boolean isLockedAdmin = p.isLockedSuperadmin()? false : p.isLockedAdmin();
		if (p.getCoordinates() != null) {
			if (p.getCoordinates().getLatitude() == 0 || p.getCoordinates().getLongitude() == 0) {
				p.setCoordinates(null);
			}
			else {
				ensureCoordinatesInDbWithElevationAndId(Lists.newArrayList(p.getCoordinates()));
			}
		}
		if (p.getId() > 0) {
			try (PreparedStatement ps = c.getConnection().prepareStatement("UPDATE ((problem p INNER JOIN sector s ON p.sector_id=s.id) INNER JOIN area a ON s.area_id=a.id) INNER JOIN user_region ur ON (a.region_id=ur.region_id AND ur.user_id=? AND (ur.admin_write=1 OR ur.superadmin_write=1)) SET p.name=?, p.rock=?, p.description=?, p.grade=?, p.fa_date=?, p.coordinates_id=?, p.broken=?, p.locked_admin=?, p.locked_superadmin=?, p.nr=?, p.type_id=?, trivia=?, starting_altitude=?, aspect=?, route_length=?, descent=?, p.trash=CASE WHEN ? THEN NOW() ELSE NULL END, p.trash_by=?, p.last_updated=now() WHERE p.id=?")) {
				ps.setInt(1, authUserId);
				ps.setString(2, trimString(p.getName()));
				ps.setString(3, trimString(p.getRock()));
				ps.setString(4, trimString(p.getComment()));
				ps.setInt(5, s.getGradeConverter().getIdGradeFromGrade(p.getOriginalGrade()));
				ps.setDate(6, dt);
				setNullablePositiveInteger(ps, 7, p.getCoordinates() == null? 0 : p.getCoordinates().getId());
				ps.setString(8, trimString(p.getBroken()));
				ps.setBoolean(9, isLockedAdmin);
				ps.setBoolean(10, p.isLockedSuperadmin());
				ps.setInt(11, p.getNr());
				ps.setInt(12, p.getT().getId());
				ps.setString(13, trimString(p.getTrivia()));
				ps.setString(14, trimString(p.getStartingAltitude()));
				ps.setString(15, trimString(p.getAspect()));
				ps.setString(16, trimString(p.getRouteLength()));
				ps.setString(17, trimString(p.getDescent()));
				ps.setBoolean(18, p.isTrash());
				ps.setInt(19, p.isTrash()? authUserId : 0);
				ps.setInt(20, p.getId());
				int res = ps.executeUpdate();
				if (res != 1) {
					throw new SQLException("Insufficient credentials");
				}
			}
			idProblem = p.getId();
		} else {
			try (PreparedStatement ps = c.getConnection().prepareStatement("INSERT INTO problem (android_id, sector_id, name, rock, description, grade, fa_date, coordinates_id, broken, locked_admin, locked_superadmin, nr, type_id, trivia, starting_altitude, aspect, route_length, descent) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", PreparedStatement.RETURN_GENERATED_KEYS)) {
				ps.setLong(1, System.currentTimeMillis());
				ps.setInt(2, p.getSectorId());
				ps.setString(3, trimString(p.getName()));
				ps.setString(4, trimString(p.getRock()));
				ps.setString(5, trimString(p.getComment()));
				ps.setInt(6, s.getGradeConverter().getIdGradeFromGrade(p.getOriginalGrade()));
				ps.setDate(7, dt);
				setNullablePositiveInteger(ps, 8, p.getCoordinates() == null? 0 : p.getCoordinates().getId());
				ps.setString(9, p.getBroken());
				ps.setBoolean(10, isLockedAdmin);
				ps.setBoolean(11, p.isLockedSuperadmin());
				ps.setInt(12, p.getNr() == 0 ? getSector(authUserId, orderByGrade, s, p.getSectorId()).getProblems().stream().map(x -> x.getNr()).mapToInt(Integer::intValue).max().orElse(0) + 1 : p.getNr());
				ps.setInt(13, p.getT().getId());
				ps.setString(14, trimString(p.getTrivia()));
				ps.setString(15, trimString(p.getStartingAltitude()));
				ps.setString(16, trimString(p.getAspect()));
				ps.setString(17, trimString(p.getRouteLength()));
				ps.setString(18, trimString(p.getDescent()));
				ps.executeUpdate();
				try (ResultSet rst = ps.getGeneratedKeys()) {
					if (rst != null && rst.next()) {
						idProblem = rst.getInt(1);
					}
				}
			}
		}
		if (idProblem == -1) {
			throw new SQLException("idProblem == -1");
		}
		// Also update last_updated on problem, sector and area
		String sqlStr = "UPDATE problem p, sector s, area a SET p.last_updated=now(), s.last_updated=now(), a.last_updated=now() WHERE p.id=? AND p.sector_id=s.id AND s.area_id=a.id";
		try (PreparedStatement ps = c.getConnection().prepareStatement(sqlStr)) {
			ps.setInt(1, idProblem);
			int res = ps.executeUpdate();
			if (res == 0) {
				throw new SQLException("Insufficient credentials");
			}
		}
		// New media
		if (p.getNewMedia() != null) {
			for (NewMedia m : p.getNewMedia()) {
				final int idSector = 0;
				final int idArea = 0;
				final int idGuestbook = 0;
				addNewMedia(authUserId, idProblem, m.getPitch(), m.isTrivia(), idSector, idArea, idGuestbook, m, multiPart);
			}
		}
		// FA
		if (p.getFa() != null) {
			Set<Integer> fas = new HashSet<>();
			try (PreparedStatement ps = c.getConnection().prepareStatement("SELECT user_id FROM fa WHERE problem_id=?")) {
				ps.setInt(1, idProblem);
				try (ResultSet rst = ps.executeQuery()) {
					while (rst.next()) {
						fas.add(rst.getInt("user_id"));
					}
				}
			}
			for (FaUser x : p.getFa()) {
				Preconditions.checkArgument(x.getId() != 0);
				if (x.getId() > 0) { // Existing user
					boolean exists = fas.remove(x.getId());
					if (!exists) {
						try (PreparedStatement ps2 = c.getConnection().prepareStatement("INSERT INTO fa (problem_id, user_id) VALUES (?, ?)")) {
							ps2.setInt(1, idProblem);
							ps2.setInt(2, x.getId());
							ps2.execute();
						}
					}
				} else { // New user
					final boolean autoCommit = false;
					int idUser = addUser(null, x.getName(), null, null, autoCommit);
					Preconditions.checkArgument(idUser > 0);
					try (PreparedStatement ps2 = c.getConnection().prepareStatement("INSERT INTO fa (problem_id, user_id) VALUES (?, ?)")) {
						ps2.setInt(1, idProblem);
						ps2.setInt(2, idUser);
						ps2.execute();
					}
				}
			}
			if (!fas.isEmpty()) {
				try (PreparedStatement ps = c.getConnection().prepareStatement("DELETE FROM fa WHERE problem_id=? AND user_id=?")) {
					for (int x : fas) {
						ps.setInt(1, idProblem);
						ps.setInt(2, x);
						ps.addBatch();
					}
					ps.executeBatch();
				}
			}
		} else {
			try (PreparedStatement ps = c.getConnection().prepareStatement("DELETE FROM fa WHERE problem_id=?")) {
				ps.setInt(1, idProblem);
			}
		}
		// Sections
		try (PreparedStatement ps = c.getConnection().prepareStatement("DELETE FROM problem_section WHERE problem_id=?")) {
			ps.setInt(1, idProblem);
			ps.execute();
		}
		if (p.getSections() != null && p.getSections().size() > 1) {
			try (PreparedStatement ps = c.getConnection().prepareStatement("INSERT INTO problem_section (problem_id, nr, description, grade) VALUES (?, ?, ?, ?)")) {
				for (ProblemSection section : p.getSections()) {
					ps.setInt(1, idProblem);
					ps.setInt(2, section.getNr());
					ps.setString(3, trimString(section.getDescription()));
					ps.setInt(4, s.getGradeConverter().getIdGradeFromGrade(section.getGrade()));
					ps.addBatch();
				}
				ps.executeBatch();
			}
		}
		// First aid ascent
		if (!s.isBouldering()) {
			try (PreparedStatement ps = c.getConnection().prepareStatement("DELETE FROM fa_aid WHERE problem_id=?")) {
				ps.setInt(1, idProblem);
				ps.execute();
			}
			try (PreparedStatement ps = c.getConnection().prepareStatement("DELETE FROM fa_aid_user WHERE problem_id=?")) {
				ps.setInt(1, idProblem);
				ps.execute();
			}
			if (p.getFaAid() != null) {
				FaAid faAid = p.getFaAid();
				final Date aidDt = Strings.isNullOrEmpty(faAid.getDate()) ? null : new Date(sdf.parse(faAid.getDate()).getTime());
				try (PreparedStatement ps = c.getConnection().prepareStatement("INSERT INTO fa_aid (problem_id, aid_date, aid_description) VALUES (?, ?, ?)")) {
					ps.setInt(1, faAid.getProblemId());
					ps.setDate(2, aidDt);
					ps.setString(3, trimString(faAid.getDescription()));
					ps.execute();
				}
				if (!faAid.getUsers().isEmpty()) {
					try (PreparedStatement ps = c.getConnection().prepareStatement("INSERT INTO fa_aid_user (problem_id, user_id) VALUES (?, ?)")) {
						for (FaUser u : faAid.getUsers()) {
							int idUser = u.getId();
							if (idUser <= 0) {
								final boolean autoCommit = false;
								idUser = addUser(null, u.getName(), null, null, autoCommit);
							}
							Preconditions.checkArgument(idUser > 0);
							ps.setInt(1, faAid.getProblemId());
							ps.setInt(2, idUser);
							ps.addBatch();
						}
						ps.executeBatch();
					}	
				}
			}
		}
		fillActivity(idProblem);
		if (p.isTrash()) {
			return new Redirect(null, 0, p.getSectorId(), 0);
		}
		return new Redirect(null, 0, 0, idProblem);
	}

	public Redirect setSector(int authUserId, boolean orderByGrade, Setup setup, Sector s, FormDataMultiPart multiPart) throws NoSuchAlgorithmException, SQLException, IOException, InterruptedException {
		int idSector = -1;
		final boolean isLockedAdmin = s.isLockedSuperadmin()? false : s.isLockedAdmin();
		boolean setPermissionRecursive = false;
		List<Coordinates> allCoordinates = new ArrayList<>();
		if (s.getOutline() != null && !s.getOutline().isEmpty()) {
			allCoordinates.addAll(s.getOutline());
		}
		if (s.getApproach() != null && s.getApproach().getCoordinates() != null && !s.getApproach().getCoordinates().isEmpty()) {
			allCoordinates.addAll(s.getApproach().getCoordinates());
		}
		if (s.getParking() != null) {
			if (s.getParking().getLatitude() == 0 || s.getParking().getLongitude() == 0) {
				s.setParking(null);
			}
			else {
				allCoordinates.add(s.getParking());
			}
		}
		ensureCoordinatesInDbWithElevationAndId(allCoordinates);
		// Sector
		if (s.getId() > 0) {
			Sector currSector = getSector(authUserId, false, setup, s.getId());
			setPermissionRecursive = currSector.isLockedAdmin() != isLockedAdmin || currSector.isLockedSuperadmin() != s.isLockedSuperadmin();
			try (PreparedStatement ps = c.getConnection().prepareStatement("UPDATE sector s, area a, user_region ur SET s.name=?, s.description=?, s.access_info=?, s.access_closed=?, s.parking_coordinates_id=?, s.locked_admin=?, s.locked_superadmin=?, s.compass_direction_id_calculated=?, s.compass_direction_id_manual=?, s.trash=CASE WHEN ? THEN NOW() ELSE NULL END, s.trash_by=? WHERE s.id=? AND s.area_id=a.id AND a.region_id=ur.region_id AND ur.user_id=? AND (ur.admin_write=1 OR ur.superadmin_write=1)")) {
				ps.setString(1, trimString(s.getName()));
				ps.setString(2, trimString(s.getComment()));
				ps.setString(3, trimString(s.getAccessInfo()));
				ps.setString(4, trimString(s.getAccessClosed()));
				setNullablePositiveInteger(ps, 5, s.getParking() == null? 0 : s.getParking().getId());
				ps.setBoolean(6, isLockedAdmin);
				ps.setBoolean(7, s.isLockedSuperadmin());
				CompassDirection calculatedWallDirection = GeoHelper.calculateCompassDirection(setup, s.getOutline());
				setNullablePositiveInteger(ps, 8, calculatedWallDirection != null? calculatedWallDirection.getId() : 0);
				setNullablePositiveInteger(ps, 9, s.getWallDirectionManual() != null? s.getWallDirectionManual().getId() : 0);
				ps.setBoolean(10, s.isTrash());
				ps.setInt(11, s.isTrash()? authUserId : 0);
				ps.setInt(12, s.getId());
				ps.setInt(13, authUserId);
				int res = ps.executeUpdate();
				if (res != 1) {
					throw new SQLException("Insufficient credentials");
				}
			}
			idSector = s.getId();

			// Problem order
			if (s.getProblemOrder() != null) {
				for (SectorProblemOrder x : s.getProblemOrder()) {
					try (PreparedStatement ps = c.getConnection().prepareStatement("UPDATE problem SET nr=? WHERE id=?")) {
						ps.setInt(1, x.getNr());
						ps.setInt(2, x.getId());
						ps.execute();
					}
				}
			}

			// Also update problems (last_updated and locked) + last_updated on area
			String sqlStr = null;
			if (setPermissionRecursive) {
				sqlStr = "UPDATE (area a INNER JOIN sector s ON a.id=s.area_id) LEFT JOIN problem p ON s.id=p.sector_id SET a.last_updated=now(), s.last_updated=now(), s.locked_admin=?, s.locked_superadmin=?, p.last_updated=now(), p.locked_admin=?, p.locked_superadmin=? WHERE s.id=?";
				try (PreparedStatement ps = c.getConnection().prepareStatement(sqlStr)) {
					ps.setBoolean(1, isLockedAdmin);
					ps.setBoolean(2, s.isLockedSuperadmin());
					ps.setBoolean(3, isLockedAdmin);
					ps.setBoolean(4, s.isLockedSuperadmin());
					ps.setInt(5, idSector);
					ps.execute();
				}
			} else {
				sqlStr = "UPDATE (area a INNER JOIN sector s ON a.id=s.area_id) LEFT JOIN problem p ON s.id=p.sector_id SET a.last_updated=now(), s.last_updated=now(), p.last_updated=now() WHERE s.id=?";
				try (PreparedStatement ps = c.getConnection().prepareStatement(sqlStr)) {
					ps.setInt(1, idSector);
					ps.execute();
				}
			}
		} else {
			ensureAdminWriteArea(authUserId, s.getAreaId());
			try (PreparedStatement ps = c.getConnection().prepareStatement("INSERT INTO sector (android_id, area_id, name, description, access_info, access_closed, parking_coordinates_id, locked_admin, locked_superadmin, compass_direction_id_calculated, compass_direction_id_manual, last_updated) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, now())", PreparedStatement.RETURN_GENERATED_KEYS)) {
				ps.setLong(1, System.currentTimeMillis());
				ps.setInt(2, s.getAreaId());
				ps.setString(3, s.getName());
				ps.setString(4, trimString(s.getComment()));
				ps.setString(5, trimString(s.getAccessInfo()));
				ps.setString(6, trimString(s.getAccessClosed()));
				setNullablePositiveInteger(ps, 7, s.getParking() == null? 0 : s.getParking().getId());
				ps.setBoolean(8, isLockedAdmin);
				ps.setBoolean(9, s.isLockedSuperadmin());
				CompassDirection calculatedWallDirection = GeoHelper.calculateCompassDirection(setup, s.getOutline());
				setNullablePositiveInteger(ps, 10, calculatedWallDirection != null? calculatedWallDirection.getId() : 0);
				setNullablePositiveInteger(ps, 11, s.getWallDirectionManual() != null? s.getWallDirectionManual().getId() : 0);
				ps.executeUpdate();
				try (ResultSet rst = ps.getGeneratedKeys()) {
					if (rst != null && rst.next()) {
						idSector = rst.getInt(1);
					}
				}
			}
		}
		Preconditions.checkArgument(idSector > 0, "idSector=" + idSector);
		// Outline
		if (s.getOutline() == null || s.getOutline().isEmpty()) {
			try (PreparedStatement ps = c.getConnection().prepareStatement("DELETE FROM sector_outline WHERE sector_id=?")) {
				ps.setInt(1, idSector);
				ps.execute();
			}
		} else {
			try (PreparedStatement ps = c.getConnection().prepareStatement("DELETE FROM sector_outline WHERE sector_id=? AND sorting>?")) {
				ps.setInt(1, idSector);
				ps.setInt(2, s.getOutline().size());
				ps.execute();
			}
			try (PreparedStatement ps = c.getConnection().prepareStatement("INSERT INTO sector_outline (sector_id, coordinates_id, sorting) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE coordinates_id=?")) {
				int sorting = 0;
				for (Coordinates coord : s.getOutline()) {
					sorting++;
					ps.setInt(1, idSector);
					ps.setInt(2, coord.getId());
					ps.setInt(3, sorting);
					ps.setInt(4, coord.getId());
					ps.addBatch();
				}
				ps.executeBatch();
			}
		}
		// Approach
		if (s.getApproach() == null || s.getApproach().getCoordinates() == null || s.getApproach().getCoordinates().isEmpty()) {
			try (PreparedStatement ps = c.getConnection().prepareStatement("DELETE FROM sector_approach WHERE sector_id=?")) {
				ps.setInt(1, idSector);
				ps.execute();
			}
		} else {
			try (PreparedStatement ps = c.getConnection().prepareStatement("DELETE FROM sector_approach WHERE sector_id=? AND sorting>?")) {
				ps.setInt(1, idSector);
				ps.setInt(2, s.getApproach().getCoordinates().size());
				ps.execute();
			}
			try (PreparedStatement ps = c.getConnection().prepareStatement("INSERT INTO sector_approach (sector_id, coordinates_id, sorting) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE coordinates_id=?")) {
				int sorting = 0;
				for (Coordinates coord : s.getApproach().getCoordinates()) {
					sorting++;
					ps.setInt(1, idSector);
					ps.setInt(2, coord.getId());
					ps.setInt(3, sorting);
					ps.setInt(4, coord.getId());
					ps.addBatch();
				}
				ps.executeBatch();
			}
		}
		// New media
		if (s.getNewMedia() != null) {
			for (NewMedia m : s.getNewMedia()) {
				final int pitch = 0;
				final int idProblem = 0;
				final int idArea = 0;
				final int idGuestbook = 0;
				addNewMedia(authUserId, idProblem, pitch, m.isTrivia(), idSector, idArea, idGuestbook, m, multiPart);
			}
		}
		Redirect res = null;
		if (s.isTrash()) {
			res = new Redirect(null, s.getAreaId(), 0, 0);
		}
		res = new Redirect(null, 0, idSector, 0);
		logger.debug("setSector() - res={}", res);
		return res;
	}

	public void setTick(int authUserId, Setup setup, Tick t) throws SQLException, ParseException {
		Preconditions.checkArgument(authUserId != -1);
		final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		// Remove from project list (if existing)
		try (PreparedStatement ps = c.getConnection().prepareStatement("DELETE FROM todo WHERE user_id=? AND problem_id=?")) {
			ps.setInt(1, authUserId);
			ps.setInt(2, t.getIdProblem());
			ps.execute();
		}
		final Date dt = Strings.isNullOrEmpty(t.getDate()) ? null : new Date(sdf.parse(t.getDate()).getTime());
		logger.debug("setTick(authUserId={}, dt={}, t={}", authUserId, dt, t);
		if (t.isDelete()) {
			Preconditions.checkArgument(t.getId() > 0, "Cannot delete a tick without id");
			try (PreparedStatement ps = c.getConnection().prepareStatement("DELETE FROM tick WHERE id=? AND user_id=? AND problem_id=?")) {
				ps.setInt(1, t.getId());
				ps.setInt(2, authUserId);
				ps.setInt(3, t.getIdProblem());
				int res = ps.executeUpdate();
				if (res != 1) {
					throw new SQLException("Invalid tick=" + t + ", authUserId=" + authUserId);
				}
			}
		}
		else if (t.getId() == -1) {
			try (PreparedStatement ps = c.getConnection().prepareStatement("INSERT INTO tick (problem_id, user_id, date, grade, comment, stars) VALUES (?, ?, ?, ?, ?, ?)", PreparedStatement.RETURN_GENERATED_KEYS)) {
				ps.setInt(1, t.getIdProblem());
				ps.setInt(2, authUserId);
				ps.setDate(3, dt);
				ps.setInt(4, setup.getGradeConverter().getIdGradeFromGrade(t.getGrade()));
				ps.setString(5, trimString(t.getComment()));
				ps.setDouble(6, t.getStars());
				ps.executeUpdate();
				try (ResultSet rst = ps.getGeneratedKeys()) {
					if (rst != null && rst.next()) {
						int idTick = rst.getInt(1);
						upsertTickRepeats(idTick, t.getRepeats());
					}
				}
			}
		}
		else if (t.getId() > 0) {
			try (PreparedStatement ps = c.getConnection().prepareStatement("UPDATE tick SET date=?, grade=?, comment=?, stars=? WHERE id=? AND problem_id=? AND user_id=?")) {
				ps.setDate(1, dt);
				ps.setInt(2, setup.getGradeConverter().getIdGradeFromGrade(t.getGrade()));
				ps.setString(3, trimString(t.getComment()));
				ps.setDouble(4, t.getStars());
				ps.setInt(5, t.getId());
				ps.setInt(6, t.getIdProblem());
				ps.setInt(7, authUserId);
				int res = ps.executeUpdate();
				if (res != 1) {
					throw new SQLException("Invalid tick=" + t + ", authUserId=" + authUserId);
				}
				upsertTickRepeats(t.getId(), t.getRepeats());
			}
		} else {
			throw new SQLException("Invalid tick=" + t + ", authUserId=" + authUserId);
		}
		fillActivity(t.getIdProblem());
	}

	public void setUserRegion(int authUserId, int regionId, boolean delete) throws SQLException {
		if (delete) {
			try (PreparedStatement ps = c.getConnection().prepareStatement("DELETE FROM user_region WHERE user_id=? AND region_id=?")) {
				ps.setInt(1, authUserId);
				ps.setInt(2, regionId);
				ps.execute();
			}
		}
		else {
			try (PreparedStatement ps = c.getConnection().prepareStatement("INSERT INTO user_region (user_id, region_id, region_visible) VALUES (?, ?, 1)")) {
				ps.setInt(1, authUserId);
				ps.setInt(2, regionId);
				ps.execute();
			}
		}
	}

	public void toggleTodo(int authUserId, int problemId) throws SQLException {
		Preconditions.checkArgument(authUserId > 0, "User not logged in");
		Preconditions.checkArgument(problemId > 0, "Problem id not set");
		int todoId = -1;
		try (PreparedStatement ps = c.getConnection().prepareStatement("SELECT id FROM todo WHERE user_id=? AND problem_id=?")) {
			ps.setInt(1, authUserId);
			ps.setInt(2, problemId);
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					todoId = rst.getInt("id");
				}
			}
		}
		if (todoId > 0) {
			try (PreparedStatement ps = c.getConnection().prepareStatement("DELETE FROM todo WHERE id=?")) {
				ps.setInt(1, todoId);
				ps.execute();
			}
		} else {
			try (PreparedStatement ps = c.getConnection().prepareStatement("INSERT INTO todo (user_id, problem_id, created) VALUES (?, ?, now())")) {
				ps.setInt(1, authUserId);
				ps.setInt(2, problemId);
				ps.execute();
			}
		}
	}

	public void trashRecover(Setup setup, int authUserId, int idArea, int idSector, int idProblem, int idMedia) throws SQLException {
		ensureSuperadminWriteRegion(authUserId, setup.getIdRegion());
		String sqlStr = null;
		int id = 0;
		// Important to check media first. A media in trash always has idArea, idSector or idProblem!
		if (idMedia > 0) {
			sqlStr = "UPDATE media SET deleted_user_id=NULL, deleted_timestamp=NULL WHERE id=?";
			id = idMedia;
		}
		else if (idArea > 0) {
			sqlStr = "UPDATE area SET trash=NULL, trash_by=NULL WHERE id=?";
			id = idArea;
		}
		else if (idSector > 0) {
			sqlStr = "UPDATE sector SET trash=NULL, trash_by=NULL WHERE id=?";
			id = idSector;
		}
		else if (idProblem > 0) {
			sqlStr = "UPDATE problem SET trash=NULL, trash_by=NULL WHERE id=?";
			id = idProblem;
		}
		try (PreparedStatement ps = c.getConnection().prepareStatement(sqlStr)) {
			ps.setInt(1, id);
			ps.execute();
		}
	}

	public void updateMediaInfo(int authUserId, MediaInfo m) throws SQLException {
		boolean ok = false;
		int areaId = 0;
		int sectorId = 0;
		int problemId = 0;
		try (PreparedStatement ps = c.getConnection().prepareStatement("SELECT ur.admin_write, ur.superadmin_write, ma.area_id, ms.sector_id, mp.problem_id FROM ((((((area a INNER JOIN sector s ON a.id=s.area_id) INNER JOIN user_region ur ON (a.region_id=ur.region_id AND ur.user_id=?)) LEFT JOIN media_area ma ON (a.id=ma.area_id AND ma.media_id=?) LEFT JOIN media_sector ms ON (s.id=ms.sector_id AND ms.media_id=?)) LEFT JOIN problem p ON s.id=p.sector_id) LEFT JOIN media_problem mp ON (p.id=mp.problem_id AND mp.media_id=?) LEFT JOIN guestbook g ON (p.id=g.problem_id)) LEFT JOIN media_guestbook mg ON (g.id=mg.guestbook_id AND mg.media_id=?)) WHERE ma.media_id IS NOT NULL OR ms.media_id IS NOT NULL OR mp.media_id IS NOT NULL OR mg.media_id IS NOT NULL GROUP BY ur.admin_write, ur.superadmin_write, ma.area_id, ms.sector_id, mp.problem_id")) {
			ps.setInt(1, authUserId);
			ps.setInt(2, m.getMediaId());
			ps.setInt(3, m.getMediaId());
			ps.setInt(4, m.getMediaId());
			ps.setInt(5, m.getMediaId());
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					ok = rst.getBoolean("admin_write") || rst.getBoolean("superadmin_write");
					areaId = rst.getInt("area_id");
					sectorId = rst.getInt("sector_id");
					problemId = rst.getInt("mp.problem_id");
				}
			}
		}
		Preconditions.checkArgument(ok, "Insufficient permissions");
		try (PreparedStatement ps = c.getConnection().prepareStatement("UPDATE media SET description=? WHERE id=?")) {
			ps.setString(1, Strings.emptyToNull(m.getDescription()));
			ps.setInt(2, m.getMediaId());
			ps.execute();
		}
		if (areaId > 0) {
			try (PreparedStatement ps = c.getConnection().prepareStatement("UPDATE media_area SET trivia=? WHERE media_id=? AND area_id=?")) {
				ps.setBoolean(1, m.isTrivia());
				ps.setInt(2, m.getMediaId());
				ps.setInt(3, areaId);
				ps.execute();
			}
		}
		else if (sectorId > 0) {
			try (PreparedStatement ps = c.getConnection().prepareStatement("UPDATE media_sector SET trivia=? WHERE media_id=? AND sector_id=?")) {
				ps.setBoolean(1, m.isTrivia());
				ps.setInt(2, m.getMediaId());
				ps.setInt(3, sectorId);
				ps.execute();
			}
		}
		else if (problemId > 0) {
			try (PreparedStatement ps = c.getConnection().prepareStatement("UPDATE media_problem SET pitch=?, trivia=? WHERE media_id=? AND problem_id=?")) {
				ps.setInt(1, m.getPitch());
				ps.setBoolean(2, m.isTrivia());
				ps.setInt(3, m.getMediaId());
				ps.setInt(4, problemId);
				ps.execute();
			}
		}
	}

	public void upsertComment(int authUserId, Setup s, Comment co, FormDataMultiPart multiPart) throws SQLException, IOException, NoSuchAlgorithmException, InterruptedException {
		Preconditions.checkArgument(authUserId > 0);
		if (co.getId() > 0) {
			List<Problem.ProblemComment> comments = getProblem(authUserId, s, co.getIdProblem(), false).getComments();
			Preconditions.checkArgument(!comments.isEmpty(), "No comment on problem " + co.getIdProblem());
			boolean commentIsEditableByUser = comments
					.stream()
					.filter(x -> x.getId() == co.getId() && x.getIdUser() == authUserId && x.isEditable())
					.findAny()
					.isPresent();
			Preconditions.checkArgument(commentIsEditableByUser, "Comment not editable by " + authUserId);
			if (co.isDelete()) {
				try (PreparedStatement ps = c.getConnection().prepareStatement("DELETE FROM guestbook WHERE id=?")) {
					ps.setInt(1, co.getId());
					ps.execute();
				}
			}
			else {
				try (PreparedStatement ps = c.getConnection().prepareStatement("UPDATE guestbook SET message=?, danger=?, resolved=? WHERE id=?")) {
					ps.setString(1, trimString(co.getComment()));
					ps.setBoolean(2, co.isDanger());
					ps.setBoolean(3, co.isResolved());
					ps.setInt(4, co.getId());
					ps.execute();
					if (co.getNewMedia() != null) {
						// New media
						for (NewMedia m : co.getNewMedia()) {
							final int idProblem = 0;
							final int idSector = 0;
							final int idArea = 0;
							addNewMedia(authUserId, idProblem, 0, m.isTrivia(), idSector, idArea, co.getId(), m, multiPart);
						}
					}
				}
			}
		} else {
			Preconditions.checkNotNull(trimString(co.getComment()));
			int parentId = 0;
			try (PreparedStatement ps = c.getConnection().prepareStatement("SELECT MIN(id) FROM guestbook WHERE problem_id=?")) {
				ps.setInt(1, co.getIdProblem());
				try (ResultSet rst = ps.executeQuery()) {
					while (rst.next()) {
						parentId = rst.getInt(1);
					}
				}
			}

			try (PreparedStatement ps = c.getConnection().prepareStatement("INSERT INTO guestbook (post_time, message, problem_id, user_id, parent_id, danger, resolved) VALUES (now(), ?, ?, ?, ?, ?, ?)", PreparedStatement.RETURN_GENERATED_KEYS)) {
				ps.setString(1, trimString(co.getComment()));
				ps.setInt(2, co.getIdProblem());
				ps.setInt(3, authUserId);
				setNullablePositiveInteger(ps, 4, parentId);
				ps.setBoolean(5, co.isDanger());
				ps.setBoolean(6, co.isResolved());
				ps.executeUpdate();
				try (ResultSet rst = ps.getGeneratedKeys()) {
					if (rst != null && rst.next()) {
						int idGuestbook = rst.getInt(1);
						if (co.getNewMedia() != null) {
							// New media
							for (NewMedia m : co.getNewMedia()) {
								final int idProblem = 0;
								final int idSector = 0;
								final int idArea = 0;
								addNewMedia(authUserId, idProblem, 0, m.isTrivia(), idSector, idArea, idGuestbook, m, multiPart);
							}
						}
					}
				}
			}
		}
		fillActivity(co.getIdProblem());
	}

	public void upsertMediaSvg(int authUserId, Setup setup, Media m) throws SQLException {
		ensureAdminWriteRegion(authUserId, setup.getIdRegion());
		// Clear existing
		try (PreparedStatement ps = c.getConnection().prepareStatement("DELETE FROM media_svg WHERE media_id=?")) {
			ps.setInt(1, m.getId());
			ps.execute();
		}
		// Insert
		for (MediaSvgElement element : m.getMediaSvgs()) {
			if (element.getT().equals(MediaSvgElement.TYPE.PATH)) {
				try (PreparedStatement ps = c.getConnection().prepareStatement("INSERT INTO media_svg (media_id, path) VALUES (?, ?)")) {
					ps.setInt(1, m.getId());
					ps.setString(2, element.getPath());
					ps.execute();
				}
			}
			else if (element.getT().equals(MediaSvgElement.TYPE.RAPPEL_BOLTED) || element.getT().equals(MediaSvgElement.TYPE.RAPPEL_NOT_BOLTED)) {
				try (PreparedStatement ps = c.getConnection().prepareStatement("INSERT INTO media_svg (media_id, rappel_x, rappel_y, rappel_bolted) VALUES (?, ?, ?, ?)")) {
					ps.setInt(1, m.getId());
					ps.setInt(2, element.getRappelX());
					ps.setInt(3, element.getRappelY());
					ps.setBoolean(4, element.getT().equals(MediaSvgElement.TYPE.RAPPEL_BOLTED));
					ps.execute();
				}
			}
			else {
				throw new RuntimeException("Invalid type: " + element.getT());
			}
		}
	}

	public void upsertPermissionUser(int regionId, int authUserId, PermissionUser u) throws SQLException {
		ensureSuperadminWriteRegion(authUserId, regionId);
		// Upsert
		try (PreparedStatement ps = c.getConnection().prepareStatement("INSERT INTO user_region (user_id, region_id, admin_read, admin_write, superadmin_read, superadmin_write) VALUES (?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE admin_read=?, admin_write=?, superadmin_read=?, superadmin_write=?")) {
			ps.setInt(1, u.getUserId());
			ps.setInt(2, regionId);
			ps.setBoolean(3, u.isAdminRead());
			ps.setBoolean(4, u.isAdminWrite());
			ps.setBoolean(5, u.isSuperadminRead());
			ps.setBoolean(6, u.isSuperadminWrite());
			ps.setBoolean(7, u.isAdminRead());
			ps.setBoolean(8, u.isAdminWrite());
			ps.setBoolean(9, u.isSuperadminRead());
			ps.setBoolean(10, u.isSuperadminWrite());
			ps.execute();
		}
		// region_visible only set by user, if user have not asked to see a specific region --> remove row
		try (PreparedStatement ps = c.getConnection().prepareStatement("DELETE FROM user_region WHERE admin_read=0 AND admin_write=0 AND superadmin_read=0 AND superadmin_write=0 AND region_visible=0")) {
			ps.execute();
		}
	}

	public void upsertSvg(int authUserId, int problemId, int mediaId, Svg svg) throws SQLException {
		ensureAdminWriteProblem(authUserId, problemId);
		// Delete/Insert/Update
		if (svg.isDelete() || trimString(svg.getPath()) == null) {
			try (PreparedStatement ps = c.getConnection().prepareStatement("DELETE FROM svg WHERE media_id=? AND problem_id=?")) {
				ps.setInt(1, mediaId);
				ps.setInt(2, problemId);
				ps.execute();
			}
		} else if (svg.getId() <= 0) {
			try (PreparedStatement ps = c.getConnection().prepareStatement("INSERT INTO svg (media_id, problem_id, path, has_anchor, anchors, texts) VALUES (?, ?, ?, ?, ?, ?)")) {
				ps.setInt(1, mediaId);
				ps.setInt(2, problemId);
				ps.setString(3, svg.getPath());
				ps.setBoolean(4, svg.isHasAnchor());
				ps.setString(5, svg.getAnchors());
				ps.setString(6, svg.getTexts());
				ps.execute();
			}
		} else {
			try (PreparedStatement ps = c.getConnection().prepareStatement("UPDATE svg SET media_id=?, problem_id=?, path=?, has_anchor=?, anchors=?, texts=? WHERE id=?")) {
				ps.setInt(1, mediaId);
				ps.setInt(2, problemId);
				ps.setString(3, svg.getPath());
				ps.setBoolean(4, svg.isHasAnchor());
				ps.setString(5, svg.getAnchors());
				ps.setString(6, svg.getTexts());
				ps.setInt(7, svg.getId());
				ps.execute();
			}
		}
	}

	private int addNewMedia(int idUser, int idProblem, int pitch, boolean trivia, int idSector, int idArea, int idGuestbook, NewMedia m, FormDataMultiPart multiPart) throws SQLException, IOException, NoSuchAlgorithmException, InterruptedException {
		int idMedia = -1;
		logger.debug("addNewMedia(idUser={}, idProblem={}, pitch={}, trivia={}, idSector={}, idArea={}, idGuestbook={}, m={}) initialized", idUser, idProblem, pitch, trivia, idSector, idArea, idGuestbook, m);
		Preconditions.checkArgument((idProblem > 0 && idSector == 0 && idArea == 0 && idGuestbook == 0)
				|| (idProblem == 0 && idSector > 0 && idArea == 0 && idGuestbook == 0)
				|| (idProblem == 0 && idSector == 0 && idArea > 0 && idGuestbook == 0)
				|| (idProblem == 0 && idSector == 0 && idArea == 0 && idGuestbook > 0));

		boolean alreadyExistsInDb = false;
		boolean isMovie = false;
		String suffix = null;
		if (Strings.isNullOrEmpty(m.getName())) {
			// Embed video url
			Preconditions.checkNotNull(m.getEmbedThumbnailUrl(), "embedThumbnailUrl required");
			Preconditions.checkNotNull(m.getEmbedVideoUrl(), "embedVideoUrl required");
			// First check if video already exists in system, don't duplicate videos!
			try (PreparedStatement ps = c.getConnection().prepareStatement("SELECT id FROM media WHERE embed_url=?")) {
				ps.setString(1, m.getEmbedVideoUrl());
				try (ResultSet rst = ps.executeQuery()) {
					while (rst.next()) {
						alreadyExistsInDb = true;
						idMedia = rst.getInt(1);
					}
				}
			}
			suffix = "mp4";
			isMovie = true;
		}
		else {
			suffix = "jpg";
			isMovie = false;
		}

		/**
		 * DB
		 */
		if (!alreadyExistsInDb) {
			try (PreparedStatement ps = c.getConnection().prepareStatement("INSERT INTO media (is_movie, suffix, photographer_user_id, uploader_user_id, date_created, description, embed_url) VALUES (?, ?, ?, ?, NOW(), ?, ?)", PreparedStatement.RETURN_GENERATED_KEYS)) {
				ps.setBoolean(1, isMovie);
				ps.setString(2, suffix);
				ps.setInt(3, getExistingOrInsertUser(m.getPhotographer()));
				ps.setInt(4, idUser);
				ps.setString(5, trimString(m.getDescription()));
				ps.setString(6, m.getEmbedVideoUrl());
				ps.executeUpdate();
				try (ResultSet rst = ps.getGeneratedKeys()) {
					if (rst != null && rst.next()) {
						idMedia = rst.getInt(1);
					}
				}
			}
		}
		Preconditions.checkArgument(idMedia > 0);
		if (idProblem > 0) {
			try (PreparedStatement ps = c.getConnection().prepareStatement("INSERT INTO media_problem (media_id, problem_id, pitch, trivia, milliseconds) VALUES (?, ?, ?, ?, ?)")) {
				ps.setInt(1, idMedia);
				ps.setInt(2, idProblem);
				setNullablePositiveInteger(ps, 3, pitch);
				ps.setBoolean(4, trivia);
				ps.setLong(5, m.getEmbedMilliseconds());
				ps.execute();
			}
		} else if (idSector > 0) {
			try (PreparedStatement ps = c.getConnection().prepareStatement("INSERT INTO media_sector (media_id, sector_id, trivia) VALUES (?, ?, ?)")) {
				ps.setInt(1, idMedia);
				ps.setInt(2, idSector);
				ps.setBoolean(3, trivia);
				ps.execute();
			}
		} else if (idArea > 0) {
			try (PreparedStatement ps = c.getConnection().prepareStatement("INSERT INTO media_area (media_id, area_id, trivia) VALUES (?, ?, ?)")) {
				ps.setInt(1, idMedia);
				ps.setInt(2, idArea);
				ps.setBoolean(3, trivia);
				ps.execute();
			}
		} else if (idGuestbook > 0) {
			try (PreparedStatement ps = c.getConnection().prepareStatement("INSERT INTO media_guestbook (media_id, guestbook_id) VALUES (?, ?)")) {
				ps.setInt(1, idMedia);
				ps.setInt(2, idGuestbook);
				ps.execute();
			}
		} else {
			throw new RuntimeException("Server error");
		}
		if (!alreadyExistsInDb) {
			if (!Strings.isNullOrEmpty(m.getInPhoto())) {
				try (PreparedStatement ps = c.getConnection().prepareStatement("INSERT INTO media_user (media_id, user_id) VALUES (?, ?)")) {
					ps.setInt(1, idMedia);
					ps.setInt(2, getExistingOrInsertUser(m.getInPhoto()));
					ps.execute();
				}
			}

			/**
			 * IO
			 */
			if (isMovie) {
				GlobalFunctions.downloadJpgFromEmbedVideo(idMedia, m.getEmbedVideoUrl());
			}
			else {
				GlobalFunctions.downloadOriginalJpgFromImage(idMedia, m.getName(), multiPart);
			}
			GlobalFunctions.createWebImagesAndUpdateDb(c, idMedia);
		}
		return idMedia;
	}

	private int addUser(String email, String firstname, String lastname, String picture, boolean autoCommit) throws SQLException, IOException {
		int id = -1;
		try (PreparedStatement ps = c.getConnection().prepareStatement("INSERT INTO user (firstname, lastname, picture) VALUES (?, ?, ?)", PreparedStatement.RETURN_GENERATED_KEYS)) {
			ps.setString(1, firstname);
			ps.setString(2, lastname);
			ps.setString(3, picture);
			ps.executeUpdate();
			try (ResultSet rst = ps.getGeneratedKeys()) {
				if (rst != null && rst.next()) {
					id = rst.getInt(1);
					logger.debug("addUser(email={}, firstname={}, lastname={}, picture={}, autoCommit={}) - getInt(1)={}", email, firstname, lastname, picture, autoCommit, id);
				}
			}
		}
		if (autoCommit) {
			c.getConnection().commit();
		}
		Preconditions.checkArgument(id > 0, "id=" + id + ", firstname=" + firstname + ", lastname=" + lastname);
		if (!Strings.isNullOrEmpty(email)) {
			try (PreparedStatement ps = c.getConnection().prepareStatement("INSERT INTO user_email (user_id, email) VALUES (?, ?)")) {
				ps.setInt(1, id);
				ps.setString(2, email.toLowerCase());
				ps.execute();
			}
		}
		if (picture != null) {
			GlobalFunctions.downloadUserImage(id, picture);
		}
		return id;
	}

	private void ensureAdminWriteArea(int authUserId, int areaId) throws SQLException {
		boolean ok = false;
		try (PreparedStatement ps = c.getConnection().prepareStatement("SELECT ur.admin_write, ur.superadmin_write FROM area a, user_region ur WHERE a.id=? AND a.region_id=ur.region_id AND ur.user_id=? AND is_readable(ur.admin_read, ur.superadmin_read, a.locked_admin, a.locked_superadmin, a.trash)=1")) {
			ps.setInt(1, areaId);
			ps.setInt(2, authUserId);
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					ok = rst.getBoolean("admin_write") || rst.getBoolean("superadmin_write");
				}
			}
		}
		Preconditions.checkArgument(ok, "Insufficient permissions");
	}

	private void ensureAdminWriteProblem(int authUserId, int problemId) throws SQLException {
		boolean ok = false;
		try (PreparedStatement ps = c.getConnection().prepareStatement("SELECT ur.admin_write, ur.superadmin_write FROM area a, sector s, problem p, user_region ur WHERE p.id=? AND a.region_id=ur.region_id AND ur.user_id=? AND a.id=s.area_id AND s.id=p.sector_id AND is_readable(ur.admin_read, ur.superadmin_read, a.locked_admin, a.locked_superadmin, a.trash)=1 AND is_readable(ur.admin_read, ur.superadmin_read, s.locked_admin, s.locked_superadmin, s.trash)=1 AND is_readable(ur.admin_read, ur.superadmin_read, p.locked_admin, p.locked_superadmin, p.trash)=1")) {
			ps.setInt(1, problemId);
			ps.setInt(2, authUserId);
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					ok = rst.getBoolean("admin_write") || rst.getBoolean("superadmin_write");
				}
			}
		}
		Preconditions.checkArgument(ok, "Insufficient permissions");
	}

	private void ensureAdminWriteRegion(int authUserId, int idRegion) throws SQLException {
		Preconditions.checkArgument(authUserId != -1, "Insufficient credentials");
		Preconditions.checkArgument(idRegion > 0, "Insufficient credentials");
		boolean ok = false;
		try (PreparedStatement ps = c.getConnection().prepareStatement("SELECT ur.admin_write, ur.superadmin_write FROM user_region ur WHERE ur.region_id=? AND ur.user_id=?")) {
			ps.setInt(1, idRegion);
			ps.setInt(2, authUserId);
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					ok = rst.getBoolean("admin_write") || rst.getBoolean("superadmin_write");
				}
			}
		}
		Preconditions.checkArgument(ok, "Insufficient permissions");
	}

	private void ensureSuperadminWriteRegion(int authUserId, int idRegion) throws SQLException {
		Preconditions.checkArgument(authUserId != -1, "Insufficient credentials");
		Preconditions.checkArgument(idRegion > 0, "Insufficient credentials");
		boolean ok = false;
		try (PreparedStatement ps = c.getConnection().prepareStatement("SELECT ur.superadmin_write FROM user_region ur WHERE ur.region_id=? AND ur.user_id=?")) {
			ps.setInt(1, idRegion);
			ps.setInt(2, authUserId);
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					ok = rst.getBoolean("superadmin_write");
				}
			}
		}
		Preconditions.checkArgument(ok, "Insufficient permissions");
	}

	private CompassDirection getCompassDirection(Setup s, int id) {
		if (id == 0) {
			return null;
		}
		return s.getCompassDirections()
				.stream()
				.filter(cd -> cd.getId() == id)
				.findAny()
				.get();
	}

	private List<CompassDirection> getCompassDirections() throws SQLException {
		List<CompassDirection> res = new ArrayList<>();
		try (PreparedStatement ps = c.getConnection().prepareStatement("SELECT id, direction FROM compass_direction ORDER BY id")) {
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					int id = rst.getInt("id");
					String direction = rst.getString("direction");
					res.add(new CompassDirection(id, direction));
				}
			}
		}
		return res;
	}

	private int getExistingOrInsertUser(String name) throws SQLException, NoSuchAlgorithmException, IOException {
		if (Strings.isNullOrEmpty(name)) {
			return 1049; // Unknown
		}
		int usId = -1;
		try (PreparedStatement ps = c.getConnection().prepareStatement("SELECT id FROM user WHERE CONCAT(firstname, ' ', COALESCE(lastname,''))=?")) {
			ps.setString(1, name);
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					usId = rst.getInt("id");
				}
			}
		}
		if (usId == -1) {
			final boolean autoCommit = false;
			usId = addUser(null, name, null, null, autoCommit);
		}
		Preconditions.checkArgument(usId > 0);
		return usId;
	}

	private Map<Integer, String> getFaAidNamesOnSector(int sectorId) throws SQLException {
		Map<Integer, String> res = new HashMap<>();
		try (PreparedStatement ps = c.getConnection().prepareStatement("SELECT p.id, group_concat(DISTINCT CONCAT(TRIM(CONCAT(u.firstname, ' ', COALESCE(u.lastname,'')))) ORDER BY u.firstname, u.lastname SEPARATOR ', ') fa FROM problem p, fa_aid_user a, user u WHERE p.sector_id=? AND p.id=a.problem_id AND a.user_id=u.id GROUP BY p.id")) {
			ps.setInt(1, sectorId);
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					int idProblem = rst.getInt("id");
					String fa = rst.getString("fa");
					res.put(idProblem, fa);
				}
			}
		}
		return res;
	}

	private List<Grade> getGrades(GRADE_SYSTEM gradeSystem) throws SQLException {
		List<Grade> res = new ArrayList<>(); 
		try (PreparedStatement ps = c.getConnection().prepareStatement("SELECT grade_id, grade FROM grade WHERE t=? ORDER BY grade_id")) {
			ps.setString(1, gradeSystem.toString());
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					int gradeId = rst.getInt("grade_id");
					String grade = rst.getString("grade");
					res.add(new Grade(gradeId, grade));
				}
			}
		}
		return res;
	}

	private List<Media> getMediaArea(int authUserId, int id, boolean inherited) throws SQLException {
		List<Media> media = new ArrayList<>();
		try (PreparedStatement ps = c.getConnection().prepareStatement("SELECT m.id, m.uploader_user_id, m.checksum, m.description, a.name location, ma.trivia, m.width, m.height, m.is_movie, m.embed_url, DATE_FORMAT(m.date_created,'%Y.%m.%d') date_created, DATE_FORMAT(m.date_taken,'%Y.%m.%d') date_taken, TRIM(CONCAT(c.firstname, ' ', COALESCE(c.lastname,''))) capturer, GROUP_CONCAT(DISTINCT TRIM(CONCAT(u.firstname, ' ', COALESCE(u.lastname,''))) ORDER BY u.firstname, u.lastname SEPARATOR ', ') tagged FROM ((((media m INNER JOIN media_area ma ON m.id=ma.media_id AND m.deleted_user_id IS NULL AND ma.area_id=?) INNER JOIN area a ON ma.area_id=a.id) INNER JOIN user c ON m.photographer_user_id=c.id) LEFT JOIN media_user mu ON m.id=mu.media_id) LEFT JOIN user u ON mu.user_id=u.id GROUP BY m.id, m.uploader_user_id, m.checksum, ma.trivia, m.description, a.name, m.width, m.height, m.is_movie, m.embed_url, ma.sorting, m.date_created, m.date_taken, c.firstname, c.lastname ORDER BY m.is_movie, m.embed_url, -ma.sorting DESC, m.id")) {
			ps.setInt(1, id);
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					int idMedia = rst.getInt("id");
					int uploaderUserId = rst.getInt("uploader_user_id");
					boolean uploadedByMe = uploaderUserId == authUserId;
					int crc32 = rst.getInt("checksum");
					String description = rst.getString("description");
					String location = rst.getString("location");
					boolean trivia = rst.getBoolean("trivia");
					if (inherited && trivia) {
						continue; // Don't inherit trivia image
					}
					int pitch = 0;
					int width = rst.getInt("width");
					int height = rst.getInt("height");
					int tyId = rst.getBoolean("is_movie") ? 2 : 1;
					String embedUrl = rst.getString("embed_url");
					String dateCreated = rst.getString("date_created");
					String dateTaken = rst.getString("date_taken");
					String capturer = rst.getString("capturer");
					String tagged = rst.getString("tagged");
					List<MediaSvgElement> mediaSvgs = getMediaSvgElements(idMedia);
					MediaMetadata mediaMetadata = new MediaMetadata(dateCreated, dateTaken, capturer, tagged, description, location);
					media.add(new Media(idMedia, uploadedByMe, crc32, pitch, trivia, width, height, tyId, null, mediaSvgs, 0, null, mediaMetadata, embedUrl, inherited, 0, 0));
				}
			}
		}
		return media;
	}

	private List<Media> getMediaGuestbook(int authUserId, int id) throws SQLException {
		List<Media> media = new ArrayList<>();
		try (PreparedStatement ps = c.getConnection().prepareStatement("SELECT m.id, m.uploader_user_id, m.checksum, m.description, CONCAT(p.name,' (',a.name,'/',s.name,')') location, m.width, m.height, m.is_movie, m.embed_url, DATE_FORMAT(m.date_created,'%Y.%m.%d') date_created, DATE_FORMAT(m.date_taken,'%Y.%m.%d') date_taken, TRIM(CONCAT(c.firstname, ' ', COALESCE(c.lastname,''))) capturer, GROUP_CONCAT(DISTINCT TRIM(CONCAT(u.firstname, ' ', COALESCE(u.lastname,''))) ORDER BY u.firstname, u.lastname SEPARATOR ', ') tagged FROM (((((((area a INNER JOIN sector s ON a.id=s.area_id) INNER JOIN problem p ON s.id=p.sector_id) INNER JOIN guestbook g ON p.id=g.problem_id) INNER JOIN media_guestbook mg ON g.id=mg.guestbook_id) INNER JOIN media m ON (mg.media_id=m.id AND m.deleted_user_id IS NULL)) INNER JOIN user c ON m.photographer_user_id=c.id) LEFT JOIN media_user mu ON m.id=mu.media_id) LEFT JOIN user u ON mu.user_id=u.id WHERE g.id=? GROUP BY m.id, m.uploader_user_id, m.checksum, a.name, s.name, m.description, m.width, m.height, m.is_movie, m.embed_url, m.date_created, m.date_taken, c.firstname, c.lastname ORDER BY m.is_movie, m.embed_url, m.id")) {
			ps.setInt(1, id);
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					int idMedia = rst.getInt("id");
					int uploaderUserId = rst.getInt("uploader_user_id");
					boolean uploadedByMe = uploaderUserId == authUserId;
					int crc32 = rst.getInt("checksum");
					String description = rst.getString("description");
					String location = rst.getString("location");
					int pitch = 0;
					boolean trivia = false;
					int width = rst.getInt("width");
					int height = rst.getInt("height");
					int tyId = rst.getBoolean("is_movie") ? 2 : 1;
					String embedUrl = rst.getString("embed_url");
					String dateCreated = rst.getString("date_created");
					String dateTaken = rst.getString("date_taken");
					String capturer = rst.getString("capturer");
					String tagged = rst.getString("tagged");
					List<MediaSvgElement> mediaSvgs = getMediaSvgElements(idMedia);
					MediaMetadata mediaMetadata = new MediaMetadata(dateCreated, dateTaken, capturer, tagged, description, location);
					media.add(new Media(idMedia, uploadedByMe, crc32, pitch, trivia, width, height, tyId, null, mediaSvgs, 0, null, mediaMetadata, embedUrl));
				}
			}
		}
		return media;
	}

	private List<Media> getMediaProblem(Setup s, int authUserId, int sectorId, int problemId, boolean showHiddenMedia) throws SQLException {
		List<Media> media = getMediaSector(s, authUserId, sectorId, problemId, true, 0, problemId, showHiddenMedia);
		try (PreparedStatement ps = c.getConnection().prepareStatement("SELECT m.id, m.uploader_user_id, m.checksum, CONCAT(p.name,' (',a.name,'/',s.name,')') location, m.description, m.width, m.height, m.is_movie, m.embed_url, DATE_FORMAT(m.date_created,'%Y.%m.%d') date_created, DATE_FORMAT(m.date_taken,'%Y.%m.%d') date_taken, mp.pitch, mp.trivia, ROUND(mp.milliseconds/1000) t, TRIM(CONCAT(c.firstname, ' ', COALESCE(c.lastname,''))) capturer, GROUP_CONCAT(DISTINCT TRIM(CONCAT(u.firstname, ' ', COALESCE(u.lastname,''))) ORDER BY u.firstname, u.lastname SEPARATOR ', ') tagged FROM ((((((area a INNER JOIN sector s ON a.id=s.area_id) INNER JOIN problem p ON s.id=p.sector_id) INNER JOIN media_problem mp ON p.id=mp.problem_id) INNER JOIN media m ON (mp.media_id=m.id AND m.deleted_user_id IS NULL)) INNER JOIN user c ON m.photographer_user_id=c.id) LEFT JOIN media_user mu ON m.id=mu.media_id) LEFT JOIN user u ON mu.user_id=u.id WHERE p.id=? GROUP BY m.id, m.uploader_user_id, m.checksum, p.name, s.name, a.name, m.description, m.width, m.height, m.is_movie, m.embed_url, mp.sorting, m.date_created, m.date_taken, mp.pitch, mp.trivia, mp.milliseconds, c.firstname, c.lastname ORDER BY m.is_movie, m.embed_url, -mp.sorting DESC, m.id")) {
			ps.setInt(1, problemId);
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					int idMedia = rst.getInt("id");
					int uploaderUserId = rst.getInt("uploader_user_id");
					boolean uploadedByMe = uploaderUserId == authUserId;
					int crc32 = rst.getInt("checksum");
					String description = rst.getString("description");
					String location = rst.getString("location");
					int pitch = rst.getInt("pitch");
					boolean trivia = rst.getBoolean("trivia");
					int width = rst.getInt("width");
					int height = rst.getInt("height");
					int tyId = rst.getBoolean("is_movie") ? 2 : 1;
					String embedUrl = rst.getString("embed_url");
					String t = rst.getString("t");
					String dateCreated = rst.getString("date_created");
					String dateTaken = rst.getString("date_taken");
					String capturer = rst.getString("capturer");
					String tagged = rst.getString("tagged");
					if (embedUrl != null) {
						long seconds = Long.parseLong(t);
						if (seconds > 0) {
							if (embedUrl.contains("youtu")) {
								embedUrl += "?start=" + seconds;
							}
							else {
								embedUrl += "#t=" + seconds + "s";
							}
						}
					}
					List<MediaSvgElement> mediaSvgs = getMediaSvgElements(idMedia);
					List<Svg> svgs = getSvgs(s, authUserId, idMedia, problemId);
					MediaMetadata mediaMetadata = new MediaMetadata(dateCreated, dateTaken, capturer, tagged, description, location);
					media.add(new Media(idMedia, uploadedByMe, crc32, pitch, trivia, width, height, tyId, t, mediaSvgs, problemId, svgs, mediaMetadata, embedUrl, false, sectorId, 0));
				}
			}
		}
		if (media != null && media.isEmpty()) {
			media = null;
		}
		return media;
	}

	private List<Media> getMediaSector(Setup s, int authUserId, int idSector, int optionalIdProblem, boolean inherited, int enableMoveToIdSector, int enableMoveToIdProblem, boolean showHiddenMedia) throws SQLException {
		List<Media> allMedia = new ArrayList<>();
		Set<Media> mediaWithRequestedTopoLine = new HashSet<>();
		String sqlStr = "SELECT m.id, m.uploader_user_id, m.checksum, ms.trivia, CONCAT(s.name,' (',a.name,')') location, m.description, m.width, m.height, m.is_movie, m.embed_url, DATE_FORMAT(m.date_created,'%Y.%m.%d') date_created, DATE_FORMAT(m.date_taken,'%Y.%m.%d') date_taken, TRIM(CONCAT(c.firstname, ' ', COALESCE(c.lastname,''))) capturer, GROUP_CONCAT(DISTINCT TRIM(CONCAT(u.firstname, ' ', COALESCE(u.lastname,''))) ORDER BY u.firstname, u.lastname SEPARATOR ', ') tagged FROM ((((area a INNER JOIN sector s ON a.id=s.area_id) INNER JOIN media_sector ms ON s.id=ms.sector_id) INNER JOIN media m ON (ms.media_id=m.id AND m.deleted_user_id IS NULL) INNER JOIN user c ON m.photographer_user_id=c.id) LEFT JOIN media_user mu ON m.id=mu.media_id) LEFT JOIN user u ON mu.user_id=u.id WHERE s.id=? GROUP BY m.id, m.uploader_user_id, m.checksum, ms.trivia, m.description, s.name, a.name, m.width, m.height, m.is_movie, m.embed_url, ms.sorting, m.date_created, m.date_taken, c.firstname, c.lastname ORDER BY m.is_movie, m.embed_url, -ms.sorting DESC, m.id";
		try (PreparedStatement ps = c.getConnection().prepareStatement(sqlStr)) {
			ps.setInt(1, idSector);
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					int idMedia = rst.getInt("id");
					int uploaderUserId = rst.getInt("uploader_user_id");
					boolean uploadedByMe = uploaderUserId == authUserId;
					int crc32 = rst.getInt("checksum");
					String description = rst.getString("description");
					String location = rst.getString("location");
					boolean trivia = rst.getBoolean("trivia");
					if (inherited && trivia) {
						continue; // Don't inherit trivia image
					}
					int pitch = 0;
					int width = rst.getInt("width");
					int height = rst.getInt("height");
					int tyId = rst.getBoolean("is_movie") ? 2 : 1;
					String embedUrl = rst.getString("embed_url");
					String dateCreated = rst.getString("date_created");
					String dateTaken = rst.getString("date_taken");
					String capturer = rst.getString("capturer");
					String tagged = rst.getString("tagged");
					List<MediaSvgElement> mediaSvgs = getMediaSvgElements(idMedia);
					List<Svg> svgs = getSvgs(s, authUserId, idMedia, optionalIdProblem);
					MediaMetadata mediaMetadata = new MediaMetadata(dateCreated, dateTaken, capturer, tagged, description, location);
					Media m = new Media(idMedia, uploadedByMe, crc32, pitch, trivia, width, height, tyId, null, mediaSvgs, optionalIdProblem, svgs, mediaMetadata, embedUrl, inherited, enableMoveToIdSector, enableMoveToIdProblem);
					if (optionalIdProblem != 0 && svgs != null && svgs.stream().filter(svg -> svg.getProblemId() == optionalIdProblem).findAny().isPresent()) {
						mediaWithRequestedTopoLine.add(m);
					}
					allMedia.add(m);
				}
			}
		}
		// Figure out what to actually return
		if (!showHiddenMedia && !mediaWithRequestedTopoLine.isEmpty()) {
			// Only images without topo lines or images with topo lines for this problem
			return allMedia.stream().filter(m -> m.getSvgs() == null || m.getSvgs().isEmpty() || mediaWithRequestedTopoLine.contains(m)).collect(Collectors.toList());
		}
		else if (!showHiddenMedia && s.isBouldering() && optionalIdProblem != 0) {
			// In bouldering we don't want to show all rocks with lines if this one does not have a line
			return allMedia.stream().filter(m -> m.getSvgs() == null || m.getSvgs().isEmpty()).collect(Collectors.toList());
		}
		return allMedia;
	}

	private List<MediaSvgElement> getMediaSvgElements(int idMedia) throws SQLException {
		List<MediaSvgElement> res = null;
		try (PreparedStatement ps = c.getConnection().prepareStatement("SELECT ms.id, ms.path, ms.rappel_x, ms.rappel_y, ms.rappel_bolted FROM media_svg ms WHERE ms.media_id=?")) {
			ps.setInt(1, idMedia);
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					if (res == null) {
						res = new ArrayList<>();
					}
					int id = rst.getInt("id");
					String path = rst.getString("path");
					if (path != null) {
						res.add(new MediaSvgElement(id, path));
					}
					else {
						int rappelX = rst.getInt("rappel_x");
						int rappelY = rst.getInt("rappel_y");
						boolean rappelBolted = rst.getBoolean("rappel_bolted");
						res.add(new MediaSvgElement(id, rappelX, rappelY, rappelBolted));
					}
				}
			}
		}
		return res;
	}

	private Map<Integer, Coordinates> getProblemCoordinates(Collection<Integer> idProblems) throws SQLException {
		Preconditions.checkArgument(!idProblems.isEmpty(), "idProblems is empty");
		Map<Integer, Coordinates> res = new HashMap<>();
		String in = ",?".repeat(idProblems.size()).substring(1);
		String sqlStr = "SELECT p.id id_problem, COALESCE(pc.id,c.id,sc.id,ac.id) coordinates_id, COALESCE(pc.latitude,c.latitude,sc.latitude,ac.latitude) latitude, COALESCE(pc.longitude,c.longitude,sc.longitude,ac.longitude) longitude, COALESCE(pc.elevation,c.elevation,sc.elevation,ac.elevation) elevation, COALESCE(pc.elevation_source,c.elevation_source,sc.elevation_source,ac.elevation_source) elevation_source FROM (((((((problem p INNER JOIN sector s ON p.sector_id=s.id) INNER JOIN area a ON s.area_id=a.id) LEFT JOIN coordinates ac ON a.coordinates_id=ac.id) LEFT JOIN coordinates sc ON s.parking_coordinates_id=sc.id) LEFT JOIN coordinates pc ON p.coordinates_id=pc.id) LEFT JOIN sector_outline so ON s.id=so.sector_id AND so.sorting=1) LEFT JOIN coordinates c ON so.coordinates_id=c.id) WHERE p.id IN (" + in + ")";
		try (PreparedStatement ps = c.getConnection().prepareStatement(sqlStr)) {
			int parameterIndex = 1;
			for (int idSector : idProblems) {
				ps.setInt(parameterIndex++, idSector);
			}
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					int idProblem = rst.getInt("id_problem");
					int idCoordinates = rst.getInt("coordinates_id");
					if (idCoordinates > 0) {
						res.put(idProblem, new Coordinates(idCoordinates, rst.getDouble("latitude"), rst.getDouble("longitude"), rst.getDouble("elevation"), rst.getString("elevation_source")));
					}
				}
			}
		}
		logger.debug("getProblemCoordinates(idProblems.size()={}) - res.size()={}", idProblems.size(), res.size());
		return res;
	}

	private Multimap<Integer, Coordinates> getRegionOutlines(Collection<Integer> idRegions) throws SQLException {
		Preconditions.checkArgument(!idRegions.isEmpty(), "idProblems is empty");
		Multimap<Integer, Coordinates> res = ArrayListMultimap.create();
		String in = ",?".repeat(idRegions.size()).substring(1);
		String sqlStr = "SELECT ro.region_id id_region, c.id, c.latitude, c.longitude, c.elevation, c.elevation_source FROM region_outline ro, coordinates c WHERE ro.region_id IN (" + in + ") AND ro.coordinates_id=c.id ORDER BY ro.sorting";
		try (PreparedStatement ps = c.getConnection().prepareStatement(sqlStr)) {
			int parameterIndex = 1;
			for (int idSector : idRegions) {
				ps.setInt(parameterIndex++, idSector);
			}
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					int idRegion = rst.getInt("id_region");
					int id = rst.getInt("id");
					double latitude = rst.getDouble("latitude");
					double longitude = rst.getDouble("longitude");
					double elevation = rst.getDouble("elevation");
					String elevationSource = rst.getString("elevation_source");
					res.put(idRegion, new Coordinates(id, latitude, longitude, elevation, elevationSource));
				}
			}
		}
		logger.debug("getRegionOutlines(idRegions.size()={}) - res.size()={}", idRegions.size(), res.size());
		return res;
	}

	private Sector getSector(int authUserId, boolean orderByGrade, Setup setup, int reqId, boolean updateHits) throws IOException, SQLException {
		Stopwatch stopwatch = Stopwatch.createStarted();
		if (updateHits) {
			try (PreparedStatement ps = c.getConnection().prepareStatement("UPDATE sector SET hits=hits+1 WHERE id=?")) {
				ps.setInt(1, reqId);
				ps.execute();
			}
		}
		Sector s = null;
		try (PreparedStatement ps = c.getConnection().prepareStatement("SELECT a.id area_id, a.locked_admin area_locked_admin, a.locked_superadmin area_locked_superadmin, a.access_info area_access_info, a.access_closed area_access_closed, a.no_dogs_allowed area_no_dogs_allowed, a.sun_from_hour area_sun_from_hour, a.sun_to_hour area_sun_to_hour, a.name area_name, CONCAT(r.url,'/sector/',s.id) canonical, s.locked_admin, s.locked_superadmin, s.name, s.description, s.access_info, s.access_closed, c.id coordinates_id, c.latitude, c.longitude, c.elevation, c.elevation_source, s.compass_direction_id_calculated, s.compass_direction_id_manual, s.hits FROM (((area a INNER JOIN region r ON a.region_id=r.id) INNER JOIN sector s ON a.id=s.area_id) LEFT JOIN coordinates c ON s.parking_coordinates_id=c.id) LEFT JOIN user_region ur ON a.region_id=ur.region_id AND ur.user_id=? WHERE s.id=? AND (r.id=? OR ur.user_id IS NOT NULL) AND is_readable(ur.admin_read, ur.superadmin_read, s.locked_admin, s.locked_superadmin, s.trash)=1 GROUP BY r.url, a.id, a.locked_admin, a.locked_superadmin, a.access_info, a.access_closed, a.no_dogs_allowed, a.sun_from_hour, a.sun_to_hour, a.name, s.locked_admin, s.locked_superadmin, s.name, s.description, s.access_info, s.access_closed, c.id, c.latitude, c.longitude, c.elevation, c.elevation_source, s.compass_direction_id_calculated, s.compass_direction_id_manual, s.hits")) {
			ps.setInt(1, authUserId);
			ps.setInt(2, reqId);
			ps.setInt(3, setup.getIdRegion());
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					int areaId = rst.getInt("area_id");
					boolean areaLockedAdmin = rst.getBoolean("area_locked_admin"); 
					boolean areaLockedSuperadmin = rst.getBoolean("area_locked_superadmin");
					String areaAccessInfo = rst.getString("area_access_info");
					String areaAccessClosed = rst.getString("area_access_closed");
					boolean areaNoDogsAllowed = rst.getBoolean("area_no_dogs_allowed");
					int areaSunFromHour = rst.getInt("area_sun_from_hour");
					int areaSunToHour = rst.getInt("area_sun_to_hour");
					String areaName = rst.getString("area_name");
					String canonical = rst.getString("canonical");
					boolean lockedAdmin = rst.getBoolean("locked_admin");
					boolean lockedSuperadmin = rst.getBoolean("locked_superadmin");
					String name = rst.getString("name");
					String comment = rst.getString("description");
					String accessInfo = rst.getString("access_info");
					String accessClosed = rst.getString("access_closed");
					int idCoordinates = rst.getInt("coordinates_id");
					Coordinates parking = idCoordinates == 0? null : new Coordinates(idCoordinates, rst.getDouble("latitude"), rst.getDouble("longitude"), rst.getDouble("elevation"), rst.getString("elevation_source"));
					List<Coordinates> sectorOutline = getSectorOutline(reqId);
					Approach sectorApproach = getSectorApproaches(Collections.singleton(reqId)).getOrDefault(reqId, null);
					CompassDirection wallDirectionCalculated = getCompassDirection(setup, rst.getInt("compass_direction_id_calculated"));
					CompassDirection wallDirectionManual = getCompassDirection(setup, rst.getInt("compass_direction_id_manual"));
					int hits = rst.getInt("hits");
					List<Media> media = null;
					List<Media> triviaMedia = null;
					List<Media> allMedia = getMediaSector(setup, authUserId, reqId, 0, false, 0, 0, false);
					allMedia.addAll(getMediaArea(authUserId, areaId, true));
					if (allMedia != null && allMedia.size() > 0) {
						media = allMedia.stream().filter(x -> !x.isTrivia()).collect(Collectors.toList());
						if (media.size() != allMedia.size()) {
							triviaMedia = allMedia.stream().filter(x -> x.isTrivia()).collect(Collectors.toList());
						}
					}

					if (media != null && media.isEmpty()) {
						media = null;
					}
					s = new Sector(null, orderByGrade, areaId, areaLockedAdmin, areaLockedSuperadmin, areaAccessInfo, areaAccessClosed, areaNoDogsAllowed, areaSunFromHour, areaSunToHour, areaName, canonical, reqId, false, lockedAdmin, lockedSuperadmin, name, comment, accessInfo, accessClosed, parking, sectorOutline, wallDirectionCalculated, wallDirectionManual, sectorApproach, media, triviaMedia, null, hits);
				}
			}
		}
		if (s == null) {
			// Sector not found, see if it's visible on a different domain
			Redirect res = c.getBuldreinfoRepo().getCanonicalUrl(0, reqId, 0);
			if (!Strings.isNullOrEmpty(res.getRedirectUrl())) {
				return new Sector(res.getRedirectUrl(), false, 0, false, false, null, null, false, 0, 0, null, null, 0, false, false, false, null, null, null, null, null, null, null, null, null, null, null, null, 0);
			}
		}

		Preconditions.checkNotNull(s, "Could not find sector with id=" + reqId);
		try (PreparedStatement ps = c.getConnection().prepareStatement("SELECT s.id, s.locked_admin, s.locked_superadmin, s.name, s.sorting FROM ((area a INNER JOIN sector s ON a.id=s.area_id) LEFT JOIN user_region ur ON a.region_id=ur.region_id AND ur.user_id=?) WHERE a.id=? AND is_readable(ur.admin_read, ur.superadmin_read, s.locked_admin, s.locked_superadmin, s.trash)=1 GROUP BY s.id, s.sorting, s.locked_admin, s.locked_superadmin, s.name, s.sorting ORDER BY s.sorting, s.name")) {
			ps.setInt(1, authUserId);
			ps.setInt(2, s.getAreaId());
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					int id = rst.getInt("id");
					boolean lockedAdmin = rst.getBoolean("locked_admin");
					boolean lockedSuperadmin = rst.getBoolean("locked_superadmin");
					String name = rst.getString("name");
					int sorting = rst.getInt("sorting");
					s.addSector(id, lockedAdmin, lockedSuperadmin, name, sorting);
				}
			}
		}
		s.orderSectors();
		for (SectorProblem sp : getSectorProblems(setup, authUserId, reqId)) {
			s.addProblem(sp);
		}
		if (!s.getProblems().isEmpty() && orderByGrade) {
			Collections.sort(s.getProblems(), Comparator.comparing(SectorProblem::getGradeNumber).reversed());
		}
		logger.debug("getSector(authUserId={}, orderByGrade={}, reqId={}) - duration={}", authUserId, orderByGrade, reqId, stopwatch);
		return s;
	}

	private Map<Integer, Approach> getSectorApproaches(Collection<Integer> idSectors) throws SQLException {
		Stopwatch stopwatch = Stopwatch.createStarted();
		Map<Integer, Approach> res = new HashMap<>();
		Preconditions.checkArgument(!idSectors.isEmpty(), "idSectors is empty");
		Multimap<Integer, Coordinates> idSectorCoordinates = ArrayListMultimap.create();
		String in = ",?".repeat(idSectors.size()).substring(1);
		String sqlStr = "SELECT sa.sector_id id_sector, c.id, c.latitude, c.longitude, c.elevation, c.elevation_source, st_distance_sphere(point(longitude, latitude), point(lag(longitude) over (partition by sa.sector_id order by sa.sorting), lag(latitude) over (partition by sa.sector_id order by sa.sorting))) m FROM sector_approach sa, coordinates c WHERE sa.sector_id IN (" + in + ") AND sa.coordinates_id=c.id ORDER BY sa.sector_id, sa.sorting";
		try (PreparedStatement ps = c.getConnection().prepareStatement(sqlStr)) {
			int parameterIndex = 1;
			for (int idSector : idSectors) {
				ps.setInt(parameterIndex++, idSector);
			}
			try (ResultSet rst = ps.executeQuery()) {
				int prevIdSector = 0;
				double distance = 0;
				while (rst.next()) {
					int idSector = rst.getInt("id_sector");
					if (prevIdSector != idSector) {
						prevIdSector = idSector;
						distance = 0;
					}
					int id = rst.getInt("id");
					double latitude = rst.getDouble("latitude");
					double longitude = rst.getDouble("longitude");
					double elevation = rst.getDouble("elevation");
					String elevationSource = rst.getString("elevation_source");
					distance += rst.getDouble("m");
					Coordinates coords = new Coordinates(id, latitude, longitude, elevation, elevationSource);
					coords.setDistance(distance);
					idSectorCoordinates.put(idSector, coords);
				}
			}
		}
		for (int idSector : idSectorCoordinates.keySet()) {
			List<Coordinates> coordinates = Lists.newArrayList(idSectorCoordinates.get(idSector));
			res.put(idSector, new Approach(coordinates));
		}
		logger.debug("getSectorApproaches(idSectors.size()={}) - res.size()={}, duration={}", idSectors.size(), res.size(), stopwatch);
		return res;
	}

	private List<Coordinates> getSectorOutline(int idSector) throws SQLException {
		Multimap<Integer, Coordinates> idSectorOutline = getSectorOutlines(Collections.singleton(idSector));
		if (idSectorOutline == null || idSectorOutline.isEmpty()) {
			return null;
		}
		return Lists.newArrayList(idSectorOutline.get(idSector));
	}

	private Multimap<Integer, Coordinates> getSectorOutlines(Collection<Integer> idSectors) throws SQLException {
		Preconditions.checkArgument(!idSectors.isEmpty(), "idSectors is empty");
		Multimap<Integer, Coordinates> res = ArrayListMultimap.create();
		String in = ",?".repeat(idSectors.size()).substring(1);
		String sqlStr = "SELECT so.sector_id id_sector, c.id, c.latitude, c.longitude, c.elevation, c.elevation_source FROM sector_outline so, coordinates c WHERE so.sector_id IN (" + in + ") AND so.coordinates_id=c.id ORDER BY so.sorting";
		try (PreparedStatement ps = c.getConnection().prepareStatement(sqlStr)) {
			int parameterIndex = 1;
			for (int idSector : idSectors) {
				ps.setInt(parameterIndex++, idSector);
			}
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					int idSector = rst.getInt("id_sector");
					int id = rst.getInt("id");
					double latitude = rst.getDouble("latitude");
					double longitude = rst.getDouble("longitude");
					double elevation = rst.getDouble("elevation");
					String elevationSource = rst.getString("elevation_source");
					res.put(idSector, new Coordinates(id, latitude, longitude, elevation, elevationSource));
				}
			}
		}
		logger.debug("getSectorOutlines(idSectors.size()={}) - res.size()={}", idSectors.size(), res.size());
		return res;
	}

	private List<SectorProblem> getSectorProblems(Setup setup, int authUserId, int sectorId) throws SQLException {
		List<SectorProblem> res = new ArrayList<>();
		Map<Integer, String> problemIdFirstAidAscentLookup = null;
		if (!setup.isBouldering()) {
			problemIdFirstAidAscentLookup = getFaAidNamesOnSector(sectorId);
		}
		String sqlStr = "SELECT p.id, p.broken, p.locked_admin, p.locked_superadmin, p.nr, p.name, p.rock, p.description, ROUND((IFNULL(SUM(nullif(t.grade,-1)),0) + p.grade) / (COUNT(CASE WHEN t.grade>0 THEN t.id END) + 1)) grade, c.id coordinates_id, c.latitude, c.longitude, c.elevation, c.elevation_source,"
				+ " COUNT(DISTINCT ps.id) num_pitches,"
				+ " COUNT(DISTINCT CASE WHEN m.is_movie=0 THEN m.id END) num_images,"
				+ " COUNT(DISTINCT CASE WHEN m.is_movie=1 THEN m.id END) num_movies,"
				+ " CASE WHEN MAX(svg.id) IS NOT NULL THEN 1 ELSE 0 END has_topo,"
				+ " group_concat(DISTINCT CONCAT(TRIM(CONCAT(u.firstname, ' ', COALESCE(u.lastname,'')))) ORDER BY u.firstname, u.lastname SEPARATOR ', ') fa,"
				+ " COUNT(DISTINCT t.id) num_ticks, ROUND(ROUND(AVG(nullif(t.stars,-1))*2)/2,1) stars,"
				+ " MAX(CASE WHEN (t.user_id=? OR u.id=?) THEN 1 END) ticked,"
				+ " CASE WHEN todo.id IS NOT NULL THEN 1 ELSE 0 END todo,"
				+ " ty.id type_id, ty.type, ty.subtype,"
				+ " danger.danger"
				+ " FROM ((((((((((((area a INNER JOIN sector s ON a.id=s.area_id) INNER JOIN problem p ON s.id=p.sector_id) INNER JOIN type ty ON p.type_id=ty.id) LEFT JOIN coordinates c ON p.coordinates_id=c.id) LEFT JOIN user_region ur ON a.region_id=ur.region_id AND ur.user_id=?) LEFT JOIN (media_problem mp LEFT JOIN media m ON (mp.media_id=m.id AND mp.trivia=0 AND m.deleted_user_id IS NULL)) ON p.id=mp.problem_id) LEFT JOIN fa f ON p.id=f.problem_id) LEFT JOIN user u ON f.user_id=u.id) LEFT JOIN tick t ON p.id=t.problem_id) LEFT JOIN todo ON (p.id=todo.problem_id AND todo.user_id=?)) LEFT JOIN (SELECT problem_id, danger FROM guestbook WHERE (danger=1 OR resolved=1) AND id IN (SELECT max(id) id FROM guestbook WHERE (danger=1 OR resolved=1) GROUP BY problem_id)) danger ON p.id=danger.problem_id) LEFT JOIN problem_section ps ON p.id=ps.problem_id) LEFT JOIN svg ON p.id=svg.problem_id"
				+ " WHERE p.sector_id=?"
				+ "   AND is_readable(ur.admin_read, ur.superadmin_read, p.locked_admin, p.locked_superadmin, p.trash)=1"
				+ " GROUP BY p.id, p.broken, p.locked_admin, p.locked_superadmin, p.nr, p.name, p.rock, p.description, p.grade, c.id, c.latitude, c.longitude, c.elevation, c.elevation_source, todo.id, ty.id, ty.type, ty.subtype, danger.danger"
				+ " ORDER BY p.nr";
		try (PreparedStatement ps = c.getConnection().prepareStatement(sqlStr)) {
			ps.setInt(1, authUserId);
			ps.setInt(2, authUserId);
			ps.setInt(3, authUserId);
			ps.setInt(4, authUserId);
			ps.setInt(5, sectorId);
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					int id = rst.getInt("id");
					String broken = rst.getString("broken");
					boolean lockedAdmin = rst.getBoolean("locked_admin");
					boolean lockedSuperadmin = rst.getBoolean("locked_superadmin");
					int nr = rst.getInt("nr");
					int grade = rst.getInt("grade");
					int idCoordinates = rst.getInt("coordinates_id");
					Coordinates coordinates = idCoordinates == 0? null : new Coordinates(idCoordinates, rst.getDouble("latitude"), rst.getDouble("longitude"), rst.getDouble("elevation"), rst.getString("elevation_source"));
					String name = rst.getString("name");
					String rock = rst.getString("rock");
					String comment = rst.getString("description");
					String fa = rst.getString("fa");
					if (problemIdFirstAidAscentLookup != null && problemIdFirstAidAscentLookup.containsKey(id)) {
						fa = "FA: " + problemIdFirstAidAscentLookup.get(id) + ". FFA: " + fa;
					}
					int numPitches = rst.getInt("num_pitches");
					boolean hasImages = rst.getInt("num_images")>0;
					boolean hasMovies = rst.getInt("num_movies")>0;
					boolean hasTopo = rst.getBoolean("has_topo");
					int numTicks = rst.getInt("num_ticks");
					double stars = rst.getDouble("stars");
					boolean ticked = rst.getBoolean("ticked");
					boolean todo = rst.getBoolean("todo");
					Type t = new Type(rst.getInt("type_id"), rst.getString("type"), rst.getString("subtype"));
					boolean danger = rst.getBoolean("danger");
					res.add(new SectorProblem(id, broken, lockedAdmin, lockedSuperadmin, nr, name, rock, comment, grade, setup.getGradeConverter().getGradeFromIdGrade(grade), fa, numPitches, hasImages, hasMovies, hasTopo, coordinates, numTicks, stars, ticked, todo, t, danger));
				}
			}
		}
		return res;
	}

	private List<Svg> getSvgs(Setup s, int authUserId, int idMedia, int optionalIdProblem) throws SQLException {
		List<Svg> res = null;
		try (PreparedStatement ps = c.getConnection().prepareStatement("WITH x AS (SELECT p.id problem_id, p.name problem_name, ROUND((IFNULL(SUM(nullif(t.grade,-1)),0) + p.grade) / (COUNT(CASE WHEN t.grade>0 THEN t.id END) + 1)) grade, pt.subtype problem_subtype, p.nr, s.id, s.path, s.has_anchor, s.texts, s.anchors, CASE WHEN p.type_id IN (1,2) THEN 1 ELSE 0 END prim, MAX(CASE WHEN t.user_id=? OR fa.user_id THEN 1 ELSE 0 END) is_ticked, CASE WHEN t2.id IS NOT NULL THEN 1 ELSE 0 END is_todo, danger is_dangerous FROM (((((svg s INNER JOIN problem p ON s.problem_id=p.id) INNER JOIN type pt ON p.type_id=pt.id) LEFT JOIN fa ON (p.id=fa.problem_id AND fa.user_id=?)) LEFT JOIN tick t ON p.id=t.problem_id) LEFT JOIN todo t2 ON p.id=t2.problem_id AND t2.user_id=?) LEFT JOIN (SELECT problem_id, danger FROM guestbook WHERE (danger=1 OR resolved=1) AND id IN (SELECT max(id) id FROM guestbook WHERE (danger=1 OR resolved=1) GROUP BY problem_id)) danger ON p.id=danger.problem_id WHERE s.media_id=? AND p.trash IS NULL GROUP BY p.id, p.name, pt.subtype, p.nr, s.id, s.path, s.has_anchor, s.texts, s.anchors, t2.id, danger.danger) SELECT x.problem_id, x.problem_name, g.grade problem_grade, g.group problem_grade_group, x.problem_subtype, x.nr, x.id, x.path, x.has_anchor, x.texts, x.anchors, x.prim, x.is_ticked, x.is_todo, x.is_dangerous FROM x INNER JOIN grade g ON x.grade=g.grade_id AND g.t=? ORDER BY x.nr")) {
			ps.setInt(1, authUserId);
			ps.setInt(2, authUserId);
			ps.setInt(3, authUserId);
			ps.setInt(4, idMedia);
			ps.setString(5, s.getGradeSystem().toString());
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					if (res == null) {
						res = new ArrayList<>();
					}
					int id = rst.getInt("id");
					int problemId = rst.getInt("problem_id");
					String problemName = rst.getString("problem_name");
					String problemGrade = rst.getString("problem_grade");
					int problemGradeGroup = rst.getInt("problem_grade_group");
					String problemSubtype = rst.getString("problem_subtype");
					int nr = rst.getInt("nr");
					String path = rst.getString("path");
					boolean hasAnchor = rst.getBoolean("has_anchor");
					String texts = rst.getString("texts");
					String anchors = rst.getString("anchors");
					boolean primary = rst.getBoolean("prim");
					boolean isTicked = rst.getBoolean("is_ticked");
					boolean isTodo = rst.getBoolean("is_todo");
					boolean isDangerous = rst.getBoolean("is_dangerous");
					res.add(new Svg(false, id, problemId, problemName, problemGrade, problemGradeGroup, problemSubtype, nr, path, hasAnchor, texts, anchors, primary, isTicked, isTodo, isDangerous));
				}
			}
		}
		return res;
	}

	private void setNullablePositiveDouble(PreparedStatement ps, int parameterIndex, double value) throws SQLException {
		if (value > 0) {
			ps.setDouble(parameterIndex, value);
		} else {
			ps.setNull(parameterIndex, Types.DOUBLE);
		}
	}

	private void setNullablePositiveInteger(PreparedStatement ps, int parameterIndex, int value) throws SQLException {
		if (value > 0) {
			ps.setInt(parameterIndex, value);
		} else {
			ps.setNull(parameterIndex, Types.INTEGER);
		}
	}

	private String trimString(String str) {
		if (str == null) {
			return null;
		}
		return Strings.emptyToNull(str.trim());
	}

	private void upsertTickRepeats(int idTick, List<TickRepeat> repeats) throws SQLException, ParseException {
		// Deleted removed ascents
		String repeatIdsToKeep = repeats == null? null : repeats.stream().filter(x -> x.getId() > 0).map(TickRepeat::getId).map(String::valueOf).collect(Collectors.joining(","));
		String sqlStr = Strings.isNullOrEmpty(repeatIdsToKeep) ? "DELETE FROM tick_repeat WHERE tick_id=?" :
			"DELETE FROM tick_repeat WHERE tick_id=? AND id NOT IN (" + repeatIdsToKeep + ")";
		try (PreparedStatement ps = c.getConnection().prepareStatement(sqlStr)) {
			ps.setInt(1, idTick);
			ps.execute();
		}
		// Upsert repeats
		if (repeats != null && !repeats.isEmpty()) {
			final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
			for (TickRepeat r : repeats) {
				final Date dt = Strings.isNullOrEmpty(r.getDate()) ? null : new Date(sdf.parse(r.getDate()).getTime());
				if (r.getId() > 0) {
					try (PreparedStatement ps = c.getConnection().prepareStatement("UPDATE tick_repeat SET date=?, comment=? WHERE id=?")) {
						ps.setDate(1, dt);
						ps.setString(2, trimString(r.getComment()));
						ps.setInt(3, r.getId());
						int res = ps.executeUpdate();
						if (res != 1) {
							throw new SQLException("Invalid repeat=" + r);
						}
					}
				}
				else {
					try (PreparedStatement ps = c.getConnection().prepareStatement("INSERT INTO tick_repeat (tick_id, date, comment) VALUES (?, ?, ?)")) {
						ps.setInt(1, idTick);
						ps.setDate(2, dt);
						ps.setString(3, trimString(r.getComment()));
						ps.execute();
					}
				}
			}
		}
	}

	private int upsertUserReturnId(String uniqueId) throws SQLException {
		int idUser = 0;
		if (Strings.isNullOrEmpty(uniqueId)) {
			return idUser;
		}
		String sqlStr = "INSERT INTO android_user (unique_id, last_sync) VALUES (?, now()) ON DUPLICATE KEY UPDATE last_sync=now()";
		try (PreparedStatement ps = c.getConnection().prepareStatement(sqlStr)) {
			ps.setString(1, uniqueId);
			ps.execute();
		}
		sqlStr = "SELECT user_id FROM android_user au WHERE unique_id=?";
		try (PreparedStatement ps = c.getConnection().prepareStatement(sqlStr)) {
			ps.setString(1, uniqueId);
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					idUser = rst.getInt(1);
				}
			}
		}
		return idUser;
	}
}