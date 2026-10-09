import 'dart:convert';

import 'package:http/http.dart' as http;

import '../models/models.dart';

/// 백엔드 API 호출
class ApiClient {
  // 에뮬레이터에서 내 PC의 localhost = 10.0.2.2
  static const String baseUrl = 'http://10.0.2.2:8080';

  final http.Client _client = http.Client();

  Future<dynamic> _get(String path) async {
    final res = await _client
        .get(Uri.parse('$baseUrl$path'))
        .timeout(const Duration(seconds: 10));
    return _decode(res);
  }

  Future<dynamic> _post(String path, Map<String, dynamic> body) async {
    final res = await _client
        .post(
          Uri.parse('$baseUrl$path'),
          headers: {'Content-Type': 'application/json'},
          body: jsonEncode(body),
        )
        .timeout(const Duration(seconds: 20));
    return _decode(res);
  }

  dynamic _decode(http.Response res) {
    final text = utf8.decode(res.bodyBytes); // 한글 깨짐 방지
    if (res.statusCode >= 400) {
      throw ApiException(res.statusCode, text);
    }
    return jsonDecode(text);
  }

  Future<List<TravelStyle>> getStyles() async {
    final list = await _get('/api/styles') as List;
    return list.map((e) => TravelStyle.fromJson(e)).toList();
  }

  Future<List<Country>> getCountries() async {
    final list = await _get('/api/countries') as List;
    return list.map((e) => Country.fromJson(e)).toList();
  }

  Future<List<Area>> getAreas(int countryId, List<int> styleIds) async {
    final list = await _get(
        '/api/countries/$countryId/areas?styles=${styleIds.join(',')}') as List;
    return list.map((e) => Area.fromJson(e)).toList();
  }

  Future<RecommendResult> recommend(TripDraft draft) async {
    final json = await _post('/api/recommend/route', {
      'countryId': draft.countryId,
      'areaIds': draft.areaIds,
      'styleIds': draft.styleIds,
      'tripDays': draft.tripDays,
    });
    return RecommendResult.fromJson(json);
  }
}

class ApiException implements Exception {
  final int status;
  final String body;

  ApiException(this.status, this.body);

  @override
  String toString() => '서버 오류 ($status)';
}

/// 앱 전체에서 같이 쓰는 인스턴스
final api = ApiClient();
