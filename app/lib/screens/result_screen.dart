import 'package:flutter/material.dart';

import '../api/api_client.dart';
import '../models/models.dart';
import '../widgets/common.dart';

/// 결과: 추천 동선 (최적 + 대안)
class ResultScreen extends StatefulWidget {
  final TripDraft draft;

  const ResultScreen({super.key, required this.draft});

  @override
  State<ResultScreen> createState() => _ResultScreenState();
}

class _ResultScreenState extends State<ResultScreen> {
  late Future<RecommendResult> _future;
  int _planIndex = 0;

  @override
  void initState() {
    super.initState();
    _future = api.recommend(widget.draft);
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('추천 동선')),
      body: FutureBuilder<RecommendResult>(
        future: _future,
        builder: (context, snap) {
          if (snap.hasError) {
            return LoadError(
                error: snap.error!,
                onRetry: () => setState(
                    () => _future = api.recommend(widget.draft)));
          }
          if (!snap.hasData) {
            return const Center(child: CircularProgressIndicator());
          }
          final result = snap.data!;
          final plans = [
            if (result.best != null) result.best!,
            ...result.alternatives,
          ];
          if (plans.isEmpty) {
            return Center(
              child: Padding(
                padding: const EdgeInsets.all(24),
                child: Text(result.summary, textAlign: TextAlign.center),
              ),
            );
          }
          final plan = plans[_planIndex.clamp(0, plans.length - 1)];
          return ListView(
            padding: const EdgeInsets.only(bottom: 24),
            children: [
              _SummaryCard(summary: result.summary),
              if (plans.length > 1)
                Padding(
                  padding: const EdgeInsets.fromLTRB(16, 4, 16, 8),
                  child: SegmentedButton<int>(
                    segments: [
                      for (var i = 0; i < plans.length; i++)
                        ButtonSegment(
                            value: i, label: Text(i == 0 ? '최적' : '대안 $i')),
                    ],
                    selected: {_planIndex},
                    onSelectionChanged: (s) =>
                        setState(() => _planIndex = s.first),
                  ),
                ),
              _PlanStats(plan: plan),
              const SizedBox(height: 8),
              ...plan.stops.map((s) => _StopTile(stop: s)),
            ],
          );
        },
      ),
    );
  }
}

class _SummaryCard extends StatelessWidget {
  final String summary;

  const _SummaryCard({required this.summary});

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Padding(
      padding: const EdgeInsets.all(16),
      child: Container(
        padding: const EdgeInsets.all(16),
        decoration: BoxDecoration(
          color: theme.colorScheme.primaryContainer,
          borderRadius: BorderRadius.circular(16),
        ),
        child: Row(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Icon(Icons.auto_awesome_rounded,
                color: theme.colorScheme.onPrimaryContainer),
            const SizedBox(width: 12),
            Expanded(
              child: Text(summary,
                  style: theme.textTheme.bodyLarge?.copyWith(
                      color: theme.colorScheme.onPrimaryContainer,
                      fontWeight: FontWeight.w600)),
            ),
          ],
        ),
      ),
    );
  }
}

class _PlanStats extends StatelessWidget {
  final Plan plan;

  const _PlanStats({required this.plan});

  @override
  Widget build(BuildContext context) {
    final items = <(String, String)>[
      ('체류', formatDays(plan.stayDays)),
      ('이동', formatMinutes(plan.travelMin)),
      ('자유일정', formatDays(plan.freeDays)),
    ];
    final theme = Theme.of(context);
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(plan.areaNames.join(' · '),
              style: theme.textTheme.titleMedium
                  ?.copyWith(fontWeight: FontWeight.bold)),
          if (plan.excludedAreaNames.isNotEmpty)
            Padding(
              padding: const EdgeInsets.only(top: 4),
              child: Text('제외: ${plan.excludedAreaNames.join(', ')}',
                  style: theme.textTheme.bodySmall
                      ?.copyWith(color: theme.colorScheme.error)),
            ),
          const SizedBox(height: 12),
          Row(
            children: items
                .map((e) => Expanded(
                      child: Container(
                        margin: const EdgeInsets.only(right: 8),
                        padding: const EdgeInsets.symmetric(vertical: 10),
                        decoration: BoxDecoration(
                          color: theme.colorScheme.surfaceContainerHighest,
                          borderRadius: BorderRadius.circular(12),
                        ),
                        child: Column(
                          children: [
                            Text(e.$1, style: theme.textTheme.labelSmall),
                            const SizedBox(height: 2),
                            Text(e.$2,
                                style: theme.textTheme.titleSmall
                                    ?.copyWith(fontWeight: FontWeight.bold)),
                          ],
                        ),
                      ),
                    ))
                .toList(),
          ),
        ],
      ),
    );
  }
}

class _StopTile extends StatelessWidget {
  final Stop stop;

  const _StopTile({required this.stop});

  (IconData, String) _mode(String? mode) => switch (mode) {
        'FLIGHT' => (Icons.flight_rounded, '항공'),
        'CAR' => (Icons.directions_car_rounded, '자동차'),
        _ => (Icons.train_rounded, '대중교통'),
      };

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final indent = stop.dayTrip ? 28.0 : 0.0;

    return Padding(
      padding: EdgeInsets.fromLTRB(16 + indent, 0, 16, 0),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // 이동 정보
          if (stop.moveMin != null)
            Padding(
              padding: const EdgeInsets.only(left: 14, top: 4, bottom: 4),
              child: Row(
                children: [
                  Icon(_mode(stop.moveMode).$1,
                      size: 16, color: theme.colorScheme.outline),
                  const SizedBox(width: 6),
                  Text(
                    stop.dayTrip
                        ? '${stop.baseRegionName}에서 당일치기 · 편도 ${formatMinutes(stop.moveMin!)}'
                        : '${_mode(stop.moveMode).$2} ${formatMinutes(stop.moveMin!)}',
                    style: theme.textTheme.bodySmall
                        ?.copyWith(color: theme.colorScheme.outline),
                  ),
                ],
              ),
            ),
          Card(
            margin: const EdgeInsets.symmetric(vertical: 4),
            child: Padding(
              padding: const EdgeInsets.all(14),
              child: Row(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  CircleAvatar(
                    radius: 14,
                    backgroundColor: stop.dayTrip
                        ? theme.colorScheme.secondaryContainer
                        : theme.colorScheme.primary,
                    child: Text('${stop.order}',
                        style: TextStyle(
                            fontSize: 12,
                            color: stop.dayTrip
                                ? theme.colorScheme.onSecondaryContainer
                                : theme.colorScheme.onPrimary)),
                  ),
                  const SizedBox(width: 12),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Row(
                          children: [
                            Expanded(
                              child: Text(stop.name,
                                  style: theme.textTheme.titleMedium
                                      ?.copyWith(fontWeight: FontWeight.bold)),
                            ),
                            Text(formatDays(stop.stayDays),
                                style: theme.textTheme.labelLarge?.copyWith(
                                    color: theme.colorScheme.primary)),
                          ],
                        ),
                        if (stop.areaName != null)
                          Text(stop.areaName!,
                              style: theme.textTheme.bodySmall),
                        if (stop.score != null) ...[
                          const SizedBox(height: 4),
                          StarRating(score: stop.score!, size: 14),
                        ],
                        if (stop.description != null) ...[
                          const SizedBox(height: 6),
                          Text(stop.description!,
                              style: theme.textTheme.bodyMedium),
                        ],
                      ],
                    ),
                  ),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }
}
