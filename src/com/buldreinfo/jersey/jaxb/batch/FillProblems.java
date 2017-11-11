package com.buldreinfo.jersey.jaxb.batch;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import com.buldreinfo.jersey.jaxb.db.ConnectionPoolProvider;
import com.buldreinfo.jersey.jaxb.db.DbConnection;
import com.buldreinfo.jersey.jaxb.helpers.GlobalFunctions;
import com.buldreinfo.jersey.jaxb.model.Area;
import com.buldreinfo.jersey.jaxb.model.FaUser;
import com.buldreinfo.jersey.jaxb.model.Problem;
import com.buldreinfo.jersey.jaxb.model.Sector;
import com.buldreinfo.jersey.jaxb.model.Type;
import com.buldreinfo.jersey.jaxb.model.User;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

public class FillProblems {
	private class Data {
		private final int typeId;
		private final int nr;
		private final String area;
		private final String sector;
		private final String problem;
		private final String comment;
		private final String grade;
		private final String fa;
		private final String faDate;
		public Data(int typeId, int nr, String area, String sector, String problem, String comment, String grade, String fa, String faDate) {
			this.typeId = typeId;
			this.nr = nr;
			this.area = area;
			this.sector = sector;
			this.problem = problem;
			this.comment = comment;
			this.grade = grade;
			this.fa = fa;
			this.faDate = faDate;
		}
		public int getTypeId() {
			return typeId;
		}
		public String getArea() {
			return area;
		}
		public String getComment() {
			return comment;
		}
		public String getFa() {
			return fa;
		}
		public String getFaDate() {
			return faDate;
		}
		public String getGrade() {
			return grade;
		}
		public int getNr() {
			return nr;
		}
		public String getProblem() {
			return problem;
		}
		public String getSector() {
			return sector;
		}
	}
	private final static String TOKEN = "ee5208f2-3384-40eb-b29f-f90143d3195f";
	private final static int REGION_ID = 4;

	public static void main(String[] args) {
		new FillProblems();
	}
	
	public FillProblems() {
		List<Data> data = new ArrayList<>();
		// FA-Date sdf="yyyy-MM-dd" TODO
//		data.add(new Data(1, "Sirev�g", "Sirev�g", "Avsporing", "(nat)", "6", "L. Jensen", null));
//		data.add(new Data(2, "Sirev�g", "Sirev�g", "Det store togr�veriet 1", "Klassisk oppvarming", "7-", "S. Brokvam & K. Juhl", null));
//		data.add(new Data(3, "Sirev�g", "Sirev�g", "Det store togr�veriet 2", null, "7+", "S. Brokvam & P. Markestad", null));
//		data.add(new Data(4, "Sirev�g", "Sirev�g", "O� est la gare", null, "7", "V. Aksnes", null));
//		data.add(new Data(5, "Sirev�g", "Sirev�g", "Gladiators Wall", "(nat)", "7", "R. Harrison", null));
//		data.add(new Data(6, "Sirev�g", "Sirev�g", "Bahnhoff Zoo", "Stilig rute", "7+/8-", "V. Aksnes", null));
//		data.add(new Data(7, "Sirev�g", "Sirev�g", "Amphitheater", "(nat)", "6", "D. Roberts", null));
//		data.add(new Data(8, "Sirev�g", "Sirev�g", "P� Skinner", null, "7", "H. J. Moe", null));
//		data.add(new Data(9, "Sirev�g", "Sirev�g", "J�rbahn", "Flott i toppen", "8-", "H. J. Moe", null));
//		data.add(new Data(10, "Sirev�g", "Sirev�g", "TGV", "Variert klatring p� store formasjoner", "6+", "H. J. Moe", null));
//		data.add(new Data(11, "Sirev�g", "Sirev�g", "Stasjonsmesteren", null, "8", "S. Brandsberg-Dahl & H. Hansen", null));
//		data.add(new Data(12, "Sirev�g", "Sirev�g", "M�rklin", null, "7-", "H. J. Moe", null));
//		data.add(new Data(13, "Sirev�g", "Sirev�g", "Sesam stasjon", "Fin klatring p� toppveggen", "8-/8", "S. Bransberg-Dahl", null));
//		data.add(new Data(14, "Sirev�g", "Sirev�g", "Fyrb�teren", "Klassikeren p� feltet", "7+", "H. Julsrud", null));
//		data.add(new Data(15, "Sirev�g", "Sirev�g", "Kvalme ved reisens slutt", "Nydelig klatring p� t�ffe og unike tak", "7+", "H. J. Moe", null));
//		data.add(new Data(16, "Sirev�g", "Sirev�g", "Maks Mekker", "Hard for graden", "7+/8-", "S. Bransberg-Dahl", null));
//		data.add(new Data(17, "Sirev�g", "Sirev�g", "Jernhesten", null, "7+/8-", "V. Aksnes", null));
//		data.add(new Data(18, "Sirev�g", "Sirev�g", "Localmotives", null, "7+", "H. J. Moe", null));
//		data.add(new Data(19, "Sirev�g", "Sirev�g", "Z", "(nat)", "7-", "T.A. S�land", null));
//		data.add(new Data(20, "Sirev�g", "Sirev�g", "Nordlandsbanen", "Pumpende!", "8", "H. J. Moe", null));
//		data.add(new Data(21, "Sirev�g", "Sirev�g", "Seniormoderasjon", null, "7-", "H. J. Moe", null));
//		data.add(new Data(22, "Sirev�g", "Sirev�g", "Judas", "(nat)", "7", "T. A. S�land", null));
//		data.add(new Data(23, "Sirev�g", "Sirev�g", "Ohm", "Tungt og styggbratt gjennom taket", "8", "S. Engelsvoll", null));
//		data.add(new Data(24, "Sirev�g", "Sirev�g", "Himmelfart", null, "7", "H. J. Moe", null));
//		data.add(new Data(25, "Sirev�g", "Sirev�g", "Fader War", "Bratt start p� t�ffe tak", "7-/7", "D.S. Kaada", null));
		try (DbConnection c = ConnectionPoolProvider.startTransaction()) {
			for (Data d : data) {
				final int idArea = upsertArea(c, d);
				final int idSector = upsertSector(c, idArea, d);
				insertProblem(c, idArea, idSector, d);
			}
			c.setSuccess();
		} catch (Exception e) {
			throw GlobalFunctions.getWebApplicationExceptionInternalError(e);
		}
	}
	
	private List<FaUser> getFas(DbConnection c, String fa) throws SQLException {
		List<FaUser> res = new ArrayList<>();
		if (!Strings.isNullOrEmpty(fa)) {
			for (String user : fa.split("&")) {
				user = user.trim();
				int id = -1;
				List<User> users = c.getBuldreinfoRepo().getUserSearch(TOKEN, user);
				if (!users.isEmpty()) {
					id = users.get(0).getId();
				}
				int ix = user.lastIndexOf(" ");
				res.add(new FaUser(id, user.substring(0, ix), user.substring(ix+1), null));
			}
		}
		return res;
	}
	
	private void insertProblem(DbConnection c, int idArea, int idSector, Data d) throws IOException, SQLException, NoSuchAlgorithmException, InterruptedException, ParseException {
		List<FaUser> fa = getFas(c, d.getFa());
		Type t = c.getBuldreinfoRepo().getTypes(REGION_ID).stream().filter(x -> x.getId() == d.getTypeId()).findFirst().get();
		Problem p = new Problem(idArea, 0, null, idSector, 0, null, 0, 0, -1, 0, d.getNr(), d.getProblem(), d.getComment(), null, d.getGrade(), d.getFaDate(), fa, 0, 0, null, 0, 0, false, null, t);
		c.getBuldreinfoRepo().setProblem(TOKEN, REGION_ID, p, null);
	}
	
	private int upsertArea(DbConnection c, Data d) throws IOException, SQLException, NoSuchAlgorithmException {
		for (Area a : c.getBuldreinfoRepo().getAreaList(TOKEN, REGION_ID)) {
			if (a.getName().equals(d.getArea())) {
				return a.getId();
			}
		}
		Area a = new Area(REGION_ID, -1, 0, d.getArea(), null, 0, 0, -1);
		a = c.getBuldreinfoRepo().setArea(TOKEN, a);
		return a.getId();
	}

	private int upsertSector(DbConnection c, int idArea, Data d) throws IOException, SQLException, NoSuchAlgorithmException, InterruptedException {
		Area a = Preconditions.checkNotNull(c.getBuldreinfoRepo().getArea(TOKEN, idArea));
		for (Area.Sector s : a.getSectors()) {
			if (s.getName().equals(d.getSector())) {
				return s.getId();
			}
		}
		Sector s = new Sector(idArea, 0, a.getName(), -1, 0, d.getSector(), null, 0, 0, null, null, null);
		s = c.getBuldreinfoRepo().setSector(TOKEN, REGION_ID, s, null);
		return s.getId();
	}
}