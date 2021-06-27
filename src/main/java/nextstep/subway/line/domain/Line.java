package nextstep.subway.line.domain;

import nextstep.subway.BaseEntity;
import nextstep.subway.station.domain.Station;
import org.springframework.util.CollectionUtils;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Entity
public class Line extends BaseEntity {
    private static final int MINIMUM_SECTION_LENGTH = 1;
    public static final String ALREADY_EXISTS_SECTION = "이미 등록된 구간 입니다.";
    public static final String NOT_ADDED_SECTION = "등록할 수 없는 구간 입니다.";
    public static final String DELETE_FAIL_SECTION = "삭제 할 수 없는 구간입니다.";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String name;
    private String color;

    @OneToMany(mappedBy = "line", cascade = {CascadeType.PERSIST, CascadeType.MERGE}, orphanRemoval = true)
    private final List<Section> sections = new ArrayList<>();

    public Line() {
    }

    public Line(String name, String color) {
        this.name = name;
        this.color = color;
    }

    public Line(String name, String color, Station upStation, Station downStation, int distance) {
        this.name = name;
        this.color = color;
        sections.add(new Section(this, upStation, downStation, distance));
    }

    public void update(Line line) {
        this.name = line.getName();
        this.color = line.getColor();
    }

    public Station findUpStation() {
        Station downStation = null;
        if (!CollectionUtils.isEmpty(sections)) {
            downStation = sections.get(0).getUpStation();
        }

        while (downStation != null) {
            Station finalDownStation = downStation;
            Optional<Section> nextLineStation = sections.stream()
                    .filter(section -> section.getDownStation().equals(finalDownStation))
                    .findFirst();

            if (!nextLineStation.isPresent()) {
                break;
            }

            downStation = nextLineStation.get().getUpStation();
        }

        return downStation;
    }

    public List<Station> stations() {
        if (sections.isEmpty()) {
            return Collections.emptyList();
        }

        List<Station> stations = new ArrayList<>();
        Station downStation = findUpStation();
        stations.add(downStation);

        while (downStation != null) {
            Station finalDownStation = downStation;
            Optional<Section> nextLineStation = sections.stream()
                    .filter(section -> section.getUpStation().equals(finalDownStation))
                    .findFirst();

            if (!nextLineStation.isPresent()) {
                break;
            }

            downStation = nextLineStation.get().getDownStation();
            stations.add(downStation);
        }

        return stations;
    }

    public void addStation(Station upStation, Station downStation, int distance) {
        List<Station> stations = this.stations();
        boolean isUpStationExisted = stations.stream().anyMatch(station -> station.equals(upStation));
        boolean isDownStationExisted = stations.stream().anyMatch(station -> station.equals(downStation));

        validateAlreadyExists(isUpStationExisted, isDownStationExisted);
        validateNotExistsStations(upStation, downStation, stations);

        if (stations.isEmpty()) {
            sections.add(new Section(this, upStation, downStation, distance));
            return;
        }

        if (isUpStationExisted) {
            updateUpStation(upStation, downStation, distance);
        } else if (isDownStationExisted) {
            updateDownStation(upStation, downStation, distance);
        }
    }

    private void validateAlreadyExists(boolean isUpStationExisted, boolean isDownStationExisted) {
        if (isUpStationExisted && isDownStationExisted) {
            throw new RuntimeException(ALREADY_EXISTS_SECTION);
        }
    }

    private void validateNotExistsStations(Station upStation, Station downStation, List<Station> stations) {
        if (!stations.isEmpty() && stations.stream().noneMatch(station -> station.equals(upStation)) &&
                stations.stream().noneMatch(station -> station.equals(downStation))) {
            throw new RuntimeException(NOT_ADDED_SECTION);
        }
    }

    private void updateUpStation(Station upStation, Station downStation, int distance) {
        sections.stream()
                .filter(section -> section.getUpStation().equals(upStation))
                .findFirst()
                .ifPresent(section -> section.updateUpStation(downStation, distance));

        sections.add(new Section(this, upStation, downStation, distance));
    }

    private void updateDownStation(Station upStation, Station downStation, int distance) {
        sections.stream()
                .filter(section -> section.getDownStation().equals(downStation))
                .findFirst()
                .ifPresent(section -> section.updateDownStation(upStation, distance));

        sections.add(new Section(this, upStation, downStation, distance));
    }

    public void removeStation(Station station) {
        validateSectionLength();

        Optional<Section> upLineStation = sections.stream()
                .filter(section -> section.getUpStation().equals(station))
                .findFirst();
        Optional<Section> downLineStation = sections.stream()
                .filter(section -> section.getDownStation().equals(station))
                .findFirst();

        if (upLineStation.isPresent() && downLineStation.isPresent()) {
            Station newUpStation = downLineStation.get().getUpStation();
            Station newDownStation = upLineStation.get().getDownStation();
            int newDistance = upLineStation.get().getDistance() + downLineStation.get().getDistance();
            sections.add(new Section(this, newUpStation, newDownStation, newDistance));
        }

        upLineStation.ifPresent(sections::remove);
        downLineStation.ifPresent(sections::remove);
    }

    private void validateSectionLength() {
        if (sections.size() <= MINIMUM_SECTION_LENGTH) {
            throw new RuntimeException(DELETE_FAIL_SECTION);
        }
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getColor() {
        return color;
    }

    public List<Section> sections() {
        return sections;
    }
}
