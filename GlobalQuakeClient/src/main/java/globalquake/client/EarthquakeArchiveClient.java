package globalquake.client;

import globalquake.core.archive.ArchivedEvent;
import globalquake.core.archive.EarthquakeArchive;
import globalquake.core.archive.ArchivedQuake;
import globalquake.core.earthquake.quality.QualityClass;
import globalquake.utils.monitorable.MonitorableCopyOnWriteArrayList;
import gqserver.api.Packet;
import gqserver.api.data.earthquake.ArchivedQuakeData;
import gqserver.api.packets.earthquake.ArchivedQuakePacket;

import java.util.List;

public class EarthquakeArchiveClient extends EarthquakeArchive {

    private final List<ArchivedQuake> archivedQuakes = new MonitorableCopyOnWriteArrayList<>();

    @Override
    public List<ArchivedQuake> getArchivedQuakes() {
        return archivedQuakes;
    }

    public void processPacket(ClientSocket socket, Packet packet) {
        if(packet instanceof ArchivedQuakePacket quakePacket){
            archiveQuake(createArchivedQuake(quakePacket), null);
        }
    }

    private ArchivedQuake createArchivedQuake(ArchivedQuakePacket quakePacket) {
        ArchivedQuakeData data = quakePacket.archivedQuakeData();
        ArchivedQuake archivedQuake = new ArchivedQuake(
                data.uuid(), data.lat(), data.lon(), data.depth(), data.magnitude(), data.origin(), QualityClass.values()[data.qualityID()]
        );

        quakePacket.archivedEventDataList().forEach(archivedEventData -> archivedQuake.getArchivedEvents().add(new ArchivedEvent(
                archivedEventData.lat(), archivedEventData.lon(), archivedEventData.maxRatio(), archivedEventData.pWave()
        )));

        return archivedQuake;
    }
}
