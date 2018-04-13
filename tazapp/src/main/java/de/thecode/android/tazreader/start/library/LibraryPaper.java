package de.thecode.android.tazreader.start.library;

import de.thecode.android.tazreader.data.Paper;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public class LibraryPaper {

    private final Paper   paper;
    private final boolean selected;
    private final int     progress;

    public LibraryPaper(Paper paper, boolean selected, int progress) {
        this.paper = paper;
        this.selected = selected;
        this.progress = progress;
    }

    public String getBookId() {
        return paper.getBookId();
    }

    public Paper getPaper() {
        return paper;
    }

    public int getProgress() {
        return progress;
    }

    public boolean isSelected() {
        return selected;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        LibraryPaper that = (LibraryPaper) o;

        return new EqualsBuilder().append(selected, that.selected)
                                  .append(progress, that.progress)
                                  .append(paper, that.paper)
                                  .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37).append(paper)
                                          .append(selected)
                                          .append(progress)
                                          .toHashCode();
    }

    @Override
    public String toString() {
        return "LibraryPaper{" +paper.getBookId() + ", downloading="+ paper.isDownloading() + ", selected=" + selected + ", progress=" + progress + '}';
    }
}
