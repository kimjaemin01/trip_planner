import 'package:flutter/material.dart';

import '../api/api_client.dart';
import '../models/models.dart';
import '../widgets/common.dart';
import 'days_screen.dart';

/// 3단계: 광역 (복수 선택, 스타일 추천도 표시)
class AreaScreen extends StatefulWidget {
  final TripDraft draft;

  const AreaScreen({super.key, required this.draft});

  @override
  State<AreaScreen> createState() => _AreaScreenState();
}

class _AreaScreenState extends State<AreaScreen> {
  late Future<List<Area>> _future;
  final Set<int> _selected = {};

  @override
  void initState() {
    super.initState();
    _load();
  }

  void _load() {
    _future = api.getAreas(widget.draft.countryId!, widget.draft.styleIds);
  }

  void _next() {
    widget.draft.areaIds = _selected.toList();
    Navigator.push(context,
        MaterialPageRoute(builder: (_) => DaysScreen(draft: widget.draft)));
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final styles = widget.draft.styleNames.join(' · ');
    return Scaffold(
      appBar: AppBar(title: Text(widget.draft.countryName ?? '')),
      body: FutureBuilder<List<Area>>(
        future: _future,
        builder: (context, snap) {
          if (snap.hasError) {
            return LoadError(
                error: snap.error!, onRetry: () => setState(_load));
          }
          if (!snap.hasData) {
            return const Center(child: CircularProgressIndicator());
          }
          final areas = snap.data!;
          return ListView(
            padding: const EdgeInsets.only(bottom: 16),
            children: [
              StepHeader(
                step: '3 / 4',
                title: '가고 싶은 지역을 골라주세요',
                subtitle: '$styles 기준 추천도 순 · 여러 곳 선택 가능',
              ),
              ...areas.map((a) {
                final selected = _selected.contains(a.areaId);
                return Padding(
                  padding:
                      const EdgeInsets.symmetric(horizontal: 16, vertical: 6),
                  child: Card(
                    clipBehavior: Clip.antiAlias,
                    color: selected ? theme.colorScheme.primaryContainer : null,
                    child: InkWell(
                      onTap: () => setState(() => selected
                          ? _selected.remove(a.areaId)
                          : _selected.add(a.areaId)),
                      child: Padding(
                        padding: const EdgeInsets.all(16),
                        child: Row(
                          children: [
                            Expanded(
                              child: Column(
                                crossAxisAlignment: CrossAxisAlignment.start,
                                children: [
                                  Text(a.name,
                                      style: theme.textTheme.titleMedium
                                          ?.copyWith(
                                              fontWeight: FontWeight.bold)),
                                  const SizedBox(height: 6),
                                  if (a.score != null)
                                    Row(
                                      children: [
                                        StarRating(score: a.score!),
                                        const SizedBox(width: 6),
                                        Text(a.score!.toStringAsFixed(1),
                                            style: theme.textTheme.labelMedium),
                                      ],
                                    ),
                                  if (a.topRegions.isNotEmpty) ...[
                                    const SizedBox(height: 6),
                                    Text('추천: ${a.topRegions.join(', ')}',
                                        style: theme.textTheme.bodySmall),
                                  ],
                                ],
                              ),
                            ),
                            Checkbox(
                              value: selected,
                              onChanged: (_) => setState(() => selected
                                  ? _selected.remove(a.areaId)
                                  : _selected.add(a.areaId)),
                            ),
                          ],
                        ),
                      ),
                    ),
                  ),
                );
              }),
            ],
          );
        },
      ),
      bottomNavigationBar: BottomActionButton(
        label: _selected.isEmpty ? '지역을 골라주세요' : '${_selected.length}곳 선택 · 다음',
        onPressed: _selected.isEmpty ? null : _next,
      ),
    );
  }
}
