all: pdf

clean:
	\rm -f *.aux *.log *.dvi *.bbl *.blg paper.pdf paperNotes.bib

paper: pdf

pdf: paper.pdf

TEXFILES = paper.tex tabs/*.tex

paper.dvi: paper.tex references.bib
	latex paper.tex
	latex paper.tex
	bibtex paper
	latex paper.tex
	latex paper.tex

paper.pdf: paper.dvi
	dvipdf -sPAPERSIZE=letter paper.dvi
