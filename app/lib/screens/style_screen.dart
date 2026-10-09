import 'package:flutter/material.dart';

import '../api/api_client.dart';
import '../models/models.dart';
import '../widgets/common.dart';
import 'country_screen.dart';

/// 1단계: 여행 스타일 (1~2개)
class StyleScreen extends StatefulWidget {
  final TripDraft draft;

  const StyleScreen({super.key, required this.draft});

  @override
  State<StyleScreen> createState() => _StyleScreenState();
}

class _StyleScreenState extends State<StyleScreen> {
  static const int maxSelect = 2;

  late Future<List<TravelStyle>> _future;
  final List<TravelStyle> _selected = [];

  @override
  void initState() {
    super.initState();
    _future = api.getStyles();
  }

  String _tendencyLabel(String t) => switch (t) {
        'MIN' => '여러 곳을 빠르게',
        'MAX' => '한 곳에 오래',
        _ => '균형 있게',
      };

  void _toggle(TravelStyle s) {
    setState(() {
      if (_selected.contains(s)) {
        _selected.remove(s);
      } else if (_selected.length < maxSelect) {
        _selected.add(s);
      } else {
        ScaffoldMessenger.of(context)
          ..hideCurrentSnackBar()
          ..showSnackBar(
              const SnackBar(content: Text('스타일은 최대 2개까지 고를 수 있어요')));
      }
    });
  }

  void _next() {
    widget.draft
      ..styleIds = _selected.map((s) => s.styleId).toList()
      ..styleNames = _selected.map((s) => s.name).toList();
    Navigator.push(context,
        MaterialPageRoute(builder: (_) => CountryScreen(draft: widget.draft)));
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Scaffold(
      appBar: AppBar(),
      body: FutureBuilder<List<TravelStyle>>(
        future: _future,
        builder: (context, snap) {
          if (snap.hasError) {
            return LoadError(
                error: snap.error!,
                onRetry: () => setState(() => _future = api.getStyles()));
          }
          if (!snap.hasData) {
            return const Center(child: CircularProgressIndicator());
          }
          final styles = snap.data!;
          return ListView(
            children: [
              const StepHeader(
                step: '1 / 4',
                title: '어떤 여행을 원하세요?',
                subtitle: '최대 2개까지 고를 수 있어요',
              ),
              Padding(
                padding: const EdgeInsets.symmetric(horizontal: 16),
                child: GridView.count(
                  crossAxisCount: 2,
                  shrinkWrap: true,
                  physics: const NeverScrollableScrollPhysics(),
                  mainAxisSpacing: 12,
                  crossAxisSpacing: 12,
                  childAspectRatio: 1.9,
                  children: styles.map((s) {
                    final selected = _selected.contains(s);
                    return InkWell(
                      borderRadius: BorderRadius.circular(16),
                      onTap: () => _toggle(s),
                      child: AnimatedContainer(
                        duration: const Duration(milliseconds: 150),
                        padding: const EdgeInsets.all(14),
                        decoration: BoxDecoration(
                          borderRadius: BorderRadius.circular(16),
                          color: selected
                              ? theme.colorScheme.primaryContainer
                              : theme.colorScheme.surfaceContainerHighest,
                          border: Border.all(
                            color: selected
                                ? theme.colorScheme.primary
                                : Colors.transparent,
                            width: 2,
                          ),
                        ),
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          mainAxisAlignment: MainAxisAlignment.center,
                          children: [
                            Row(
                              children: [
                                Expanded(
                                  child: Text(s.name,
                                      style: theme.textTheme.titleMedium
                                          ?.copyWith(
                                              fontWeight: FontWeight.bold)),
                                ),
                                if (selected)
                                  Icon(Icons.check_circle_rounded,
                                      size: 20,
                                      color: theme.colorScheme.primary),
                              ],
                            ),
                            const SizedBox(height: 4),
                            Text(_tendencyLabel(s.stayTendency),
                                style: theme.textTheme.bodySmall),
                          ],
                        ),
                      ),
                    );
                  }).toList(),
                ),
              ),
            ],
          );
        },
      ),
      bottomNavigationBar: BottomActionButton(
        label: _selected.isEmpty ? '스타일을 골라주세요' : '다음',
        onPressed: _selected.isEmpty ? null : _next,
      ),
    );
  }
}
