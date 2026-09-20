package com.resumeanalyzer.backend.service;

import com.resumeanalyzer.backend.dto.AnalysisResponse;
import com.resumeanalyzer.backend.dto.CategoryScore;
import com.resumeanalyzer.backend.model.Resume;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ScoringService {

    // ---------- Patterns ----------
    private static final Pattern EMAIL =
            Pattern.compile("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}");
    private static final Pattern PHONE_CANDIDATE =
            Pattern.compile("\\+?\\d[\\d\\s().-]{8,}\\d");
    // PDFs often hold links as the word "LinkedIn", not the URL, so we look for the word
    private static final Pattern LINKEDIN = Pattern.compile("linkedin", Pattern.CASE_INSENSITIVE);
    private static final Pattern GITHUB = Pattern.compile("github", Pattern.CASE_INSENSITIVE);
    private static final Pattern METRIC = Pattern.compile(
            "\\b\\d[\\d,.]*\\s?(%|\\+|x\\b|k\\b|ms\\b|users\\b|requests\\b|customers\\b|records\\b)",
            Pattern.CASE_INSENSITIVE);

    // Lines about grades/boards are not achievements, so they are ignored when counting metrics
    private static final Pattern EDU_LINE = Pattern.compile(
            "\\b(cgpa|gpa|cbse|icse|percentile|board|marks|grade)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern WEAK_PHRASE = Pattern.compile(
            "\\b(responsible for|worked on|helped with|duties included|assisted in|tasked with)\\b",
            Pattern.CASE_INSENSITIVE);
    // "I" not followed by "/" (so "I/O" is fine), plus lowercase "my" and "me"
    private static final Pattern FIRST_PERSON = Pattern.compile("\\bI\\b(?!/)|\\b(my|me)\\b");

    private static final int FLAGS = Pattern.CASE_INSENSITIVE | Pattern.MULTILINE;
    private static final Map<String, Pattern> SECTIONS = new LinkedHashMap<>();
    static {
        SECTIONS.put("Education", Pattern.compile("^\\s*(education|academics?|qualifications?)\\b", FLAGS));
        SECTIONS.put("Experience", Pattern.compile("^\\s*((work|professional)\\s+)?(experience|employment|internships?|work history)\\b", FLAGS));
        SECTIONS.put("Skills", Pattern.compile("^\\s*(technical\\s+|key\\s+|core\\s+)?(skills|technologies|competencies)\\b", FLAGS));
        SECTIONS.put("Projects", Pattern.compile("^\\s*(personal\\s+|academic\\s+|key\\s+)?projects?\\b", FLAGS));
    }

    // "|" separates aliases. The first name is the one shown to the user.
    private static final List<String> SKILLS = List.of(
            "Java", "Spring Boot|SpringBoot", "Hibernate", "JPA", "REST API|RESTful|REST APIs",
            "Microservices", "Python", "JavaScript", "TypeScript", "React", "Angular", "Node.js",
            "Flutter", "Dart", "Kotlin", "Swift", "C++", "C#", "SQL", "MySQL", "PostgreSQL|Postgres",
            "MongoDB", "Redis", "Docker", "Kubernetes|K8s", "AWS|Amazon Web Services", "Azure", "GCP",
            "Git", "GitHub", "Linux", "CI/CD|CICD", "Jenkins", "Maven", "JUnit", "HTML", "CSS",
            "Firebase", "Data Structures", "Algorithms", "OOP", "Machine Learning", "Postman",
            "Agile", "Jira"
    );

    // Skills recruiters often look for. We flag the ones missing (job matching comes in Phase 6).
    private static final List<String> IN_DEMAND = List.of(
            "Docker", "REST API", "SQL", "Git", "AWS", "Spring Boot",
            "Microservices", "JUnit", "CI/CD", "Kubernetes"
    );

    private static final List<String> ACTION_VERBS = List.of(
            "developed", "built", "designed", "implemented", "led", "created", "optimized",
            "optimised", "improved", "reduced", "increased", "automated", "deployed", "integrated",
            "managed", "launched", "engineered", "analyzed", "migrated", "collaborated", "delivered",
            "tested", "refactored", "mentored", "established", "streamlined", "achieved",
            "architected", "configured", "resolved", "enhanced", "spearheaded", "coordinated"
    );

    public AnalysisResponse analyze(Resume resume) {
        String text = resume.getExtractedText();
        String lower = text.toLowerCase();

        List<CategoryScore> categories = new ArrayList<>();
        List<String> strengths = new ArrayList<>();
        List<String> suggestions = new ArrayList<>();

        // ---------- 1. Contact info (10) ----------
        int contact = 0;
        if (EMAIL.matcher(text).find()) contact += 3;
        else suggestions.add("Add a professional email address at the top of your resume.");
        if (hasPhone(text)) contact += 3;
        else suggestions.add("Add a phone number so recruiters can reach you.");
        if (LINKEDIN.matcher(text).find()) contact += 2;
        else suggestions.add("Add your LinkedIn profile link.");
        if (GITHUB.matcher(text).find()) contact += 2;
        else suggestions.add("Add your GitHub link so recruiters can see your code.");
        categories.add(new CategoryScore("Contact info", contact, 10));
        if (contact >= 8) strengths.add("Complete contact details.");

        // ---------- 2. Sections (20) ----------
        int sections = 0;
        for (Map.Entry<String, Pattern> entry : SECTIONS.entrySet()) {
            if (entry.getValue().matcher(text).find()) {
                sections += 5;
            } else {
                suggestions.add("Add a clear \"" + entry.getKey() + "\" section heading.");
            }
        }
        categories.add(new CategoryScore("Sections", sections, 20));
        if (sections == 20) strengths.add("All key sections are present: Education, Experience, Skills, Projects.");

        // ---------- 3. Skills (25) ----------
        List<String> foundSkills = new ArrayList<>();
        for (String skill : SKILLS) {
            if (hasSkill(lower, skill)) foundSkills.add(canonicalName(skill));
        }
        int skillScore = Math.min(25, Math.round(foundSkills.size() * 25f / 15));
        categories.add(new CategoryScore("Skills", skillScore, 25));
        if (skillScore >= 20) {
            strengths.add("Strong skills coverage (" + foundSkills.size() + " recognised skills).");
        } else {
            suggestions.add("List more relevant technical skills. Only " + foundSkills.size()
                    + " recognised skills were found.");
        }

        List<String> missingKeywords = new ArrayList<>();
        for (String wanted : IN_DEMAND) {
            if (!foundSkills.contains(wanted)) missingKeywords.add(wanted);
            if (missingKeywords.size() == 5) break;
        }

        // ---------- 4. Impact (30) ----------
        // Treat each line with 6+ words as a "content line" (after stripping bullet symbols)
        List<String> contentLines = new ArrayList<>();
        for (String line : text.split("\\R")) {
            String cleaned = line.replaceFirst("^[\\s\\u2022\\u2013\\u25AA\\u25CF\\u25CB\\u25E6*\\-]+", "").trim();
            if (!cleaned.isEmpty() && cleaned.split("\\s+").length >= 6) contentLines.add(cleaned);
        }

        // 4a. Share of lines that START with an action verb (12 pts)
        int verbLines = 0;
        for (String line : contentLines) {
            String first = line.split("\\s+")[0].toLowerCase().replaceAll("[^a-z]", "");
            if (ACTION_VERBS.contains(first)) verbLines++;
        }
        double verbRatio = contentLines.isEmpty() ? 0 : (double) verbLines / contentLines.size();
        int verbScore = (int) Math.round(Math.min(1.0, verbRatio / 0.35) * 12);

        // 4b. Lines that show a measurable result, ignoring CGPA/board-marks lines (12 pts)
        int metricLines = 0;
        for (String line : contentLines) {
            if (METRIC.matcher(line).find() && !EDU_LINE.matcher(line).find()) metricLines++;
        }
        int metricScore = Math.min(12, metricLines * 2);

        // 4c. Weak phrases cost points (6 pts, minus 2 per weak phrase)
        int weak = 0;
        Matcher wm = WEAK_PHRASE.matcher(text);
        while (wm.find()) weak++;
        int weakScore = Math.max(0, 6 - weak * 2);

        // Temporary debug line: shows what the scorer measured (remove when tuning is done)
        System.out.printf("DEBUG verbRatio=%.2f metricLines=%d weak=%d%n", verbRatio, metricLines, weak);

        categories.add(new CategoryScore("Impact", verbScore + metricScore + weakScore, 30));
        if (verbRatio >= 0.35) {
            strengths.add("Most bullet points start with strong action verbs.");
        } else {
            suggestions.add("Only " + Math.round(verbRatio * 100) + "% of your lines start with an action verb. "
                    + "Begin bullets with verbs like \"Built\", \"Optimized\" or \"Automated\".");
        }
        if (metricLines >= 6) {
            strengths.add("Good use of measurable results (" + metricLines + " lines with numbers).");
        } else {
            suggestions.add("Only " + metricLines + " lines show measurable results. Aim for 6 or more, "
                    + "e.g. \"Reduced API response time by 30%\".");
        }
        if (weak > 0) {
            suggestions.add("Replace weak phrases like \"responsible for\" or \"worked on\" with action verbs.");
        }

        // ---------- 5. Length & style (15) ----------
        int words = text.trim().split("\\s+").length;
        int lengthPoints;
        if (words >= 300 && words <= 800) {
            lengthPoints = 10;
            strengths.add("Good length (" + words + " words), about 1 to 2 pages.");
        } else if ((words >= 200 && words < 300) || (words > 800 && words <= 1000)) {
            lengthPoints = 6;
        } else {
            lengthPoints = 2;
        }
        if (words < 300) suggestions.add("Your resume is short (" + words + " words). Add more detail about projects and results.");
        if (words > 800) suggestions.add("Your resume is long (" + words + " words). Try to keep it to 1 or 2 pages.");

        int stylePoints = FIRST_PERSON.matcher(text).find() ? 0 : 5;
        if (stylePoints == 0) {
            suggestions.add("Avoid first-person words like \"I\" and \"my\". Resumes read better without them.");
        }
        categories.add(new CategoryScore("Length & style", lengthPoints + stylePoints, 15));

        // ---------- Total ----------
        int overall = categories.stream().mapToInt(CategoryScore::score).sum();

        return new AnalysisResponse(resume.getId(), resume.getFileName(), overall,
                categories, strengths, suggestions, missingKeywords);
    }

    // ---------- Helpers ----------
    private boolean hasPhone(String text) {
        Matcher m = PHONE_CANDIDATE.matcher(text);
        while (m.find()) {
            long digits = m.group().chars().filter(Character::isDigit).count();
            if (digits >= 10 && digits <= 13) return true;   // rejects years like "2021 - 2025"
        }
        return false;
    }

    private boolean hasSkill(String lowerText, String skillEntry) {
        for (String alias : skillEntry.split("\\|")) {
            // The lookarounds stop "Java" matching inside "JavaScript" and "SQL" inside "MySQL"
            Pattern p = Pattern.compile("(?<![a-z0-9])" + Pattern.quote(alias.trim().toLowerCase()) + "(?![a-z0-9])");
            if (p.matcher(lowerText).find()) return true;
        }
        return false;
    }

    private String canonicalName(String skillEntry) {
        return skillEntry.split("\\|")[0];
    }
}