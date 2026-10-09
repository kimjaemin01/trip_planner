/// 백엔드 응답 모델

double? _toDouble(dynamic v) => v == null ? null : (v as num).toDouble();

List<String> _toStringList(dynamic v) =>
    v == null ? const [] : (v as List).map((e) => e.toString()).toList();

class TravelStyle {
  final int styleId;
  final String name;
  final int dailyVisitCount;
  final String stayTendency; // MIN / REC / MAX

  TravelStyle.fromJson(Map<String, dynamic> j)
      : styleId = j['styleId'],
        name = j['name'],
        dailyVisitCount = j['dailyVisitCount'],
        stayTendency = j['stayTendency'];
}

class Country {
  final int countryId;
  final String name;
  final String? currency;
  final String? imageUrl;

  Country.fromJson(Map<String, dynamic> j)
      : countryId = j['countryId'],
        name = j['name'],
        currency = j['currency'],
        imageUrl = j['imageUrl'];
}

class Area {
  final int areaId;
  final String name;
  final List<String> mainAirports;
  final double? score;
  final List<String> topRegions;

  Area.fromJson(Map<String, dynamic> j)
      : areaId = j['areaId'],
        name = j['name'],
        mainAirports = _toStringList(j['mainAirports']),
        score = _toDouble(j['score']),
        topRegions = _toStringList(j['topRegions']);
}

class Stop {
  final int order;
  final int regionId;
  final String name;
  final String? areaName;
  final double stayDays;
  final double? score;
  final String? description;
  final bool dayTrip;
  final String? baseRegionName;
  final String? moveMode; // TRANSIT / CAR / FLIGHT
  final int? moveMin;

  Stop.fromJson(Map<String, dynamic> j)
      : order = j['order'],
        regionId = j['regionId'],
        name = j['name'],
        areaName = j['areaName'],
        stayDays = _toDouble(j['stayDays']) ?? 0,
        score = _toDouble(j['score']),
        description = j['description'],
        dayTrip = j['dayTrip'] ?? false,
        baseRegionName = j['baseRegionName'],
        moveMode = j['moveMode'],
        moveMin = j['moveMin'];
}

class Plan {
  final List<String> areaNames;
  final List<String> excludedAreaNames;
  final double stayDays;
  final int travelMin;
  final double travelDays;
  final double freeDays;
  final double totalScore;
  final List<Stop> stops;

  Plan.fromJson(Map<String, dynamic> j)
      : areaNames = _toStringList(j['areaNames']),
        excludedAreaNames = _toStringList(j['excludedAreaNames']),
        stayDays = _toDouble(j['stayDays']) ?? 0,
        travelMin = j['travelMin'] ?? 0,
        travelDays = _toDouble(j['travelDays']) ?? 0,
        freeDays = _toDouble(j['freeDays']) ?? 0,
        totalScore = _toDouble(j['totalScore']) ?? 0,
        stops = (j['stops'] as List? ?? const [])
            .map((e) => Stop.fromJson(e))
            .toList();
}

class RecommendResult {
  final int tripDays;
  final double usableDays;
  final String summary;
  final Plan? best;
  final List<Plan> alternatives;

  RecommendResult.fromJson(Map<String, dynamic> j)
      : tripDays = j['tripDays'],
        usableDays = _toDouble(j['usableDays']) ?? 0,
        summary = j['summary'] ?? '',
        best = j['best'] == null ? null : Plan.fromJson(j['best']),
        alternatives = (j['alternatives'] as List? ?? const [])
            .map((e) => Plan.fromJson(e))
            .toList();
}

/// 화면을 넘어가며 채우는 여행 계획 입력값
class TripDraft {
  List<int> styleIds = [];
  List<String> styleNames = [];
  int? countryId;
  String? countryName;
  List<int> areaIds = [];
  int tripDays = 5;
}
