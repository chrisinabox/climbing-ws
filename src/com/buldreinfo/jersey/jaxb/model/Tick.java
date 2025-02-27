package com.buldreinfo.jersey.jaxb.model;

import java.util.List;

public class Tick {
	private final boolean delete;
	private final int id;
	private final int idProblem;
	private final String comment;
	private final String date;
	private final double stars;
	private final String grade;
	private final List<TickRepeat> repeats;
	
	public Tick(boolean delete, int id, int idProblem, String comment, String date, double stars, String grade, List<TickRepeat> repeats) {
		this.delete = delete;
		this.id = id;
		this.idProblem = idProblem;
		this.comment = comment;
		this.date = date;
		this.stars = stars;
		this.grade = grade;
		this.repeats = repeats;
	}
	
	public String getComment() {
		return comment;
	}

	public String getDate() {
		return date;
	}

	public String getGrade() {
		return grade;
	}

	public int getId() {
		return id;
	}

	public int getIdProblem() {
		return idProblem;
	}

	public List<TickRepeat> getRepeats() {
		return repeats;
	}

	public double getStars() {
		return stars;
	}
	
	public boolean isDelete() {
		return delete;
	}

	@Override
	public String toString() {
		return "Tick [delete=" + delete + ", id=" + id + ", idProblem=" + idProblem + ", comment=" + comment + ", date="
				+ date + ", stars=" + stars + ", grade=" + grade + ", repeats=" + repeats + "]";
	}
}