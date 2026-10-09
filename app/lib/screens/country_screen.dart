import 'package:flutter/material.dart';

import '../api/api_client.dart';
import '../models/models.dart';
import '../widgets/common.dart';
import 'area_screen.dart';

/// 2단계: 국가 (1개)
class CountryScreen extends StatefulWidget {
  final TripDraft draft;

  const CountryScreen({super.key, required this.draft});

  @override
  State<CountryScreen> createState() => _CountryScreenState();
}

class _CountryScreenState extends State<CountryScreen> {
  late Future<List<Country>> _future;

  @override
  void initState() {
    super.initState();
    _future = api.getCountries();
  }

  void _select(Country c) {
    widget.draft
      ..countryId = c.countryId
      ..countryName = c.name
      ..areaIds = [];
    Navigator.push(context,
        MaterialPageRoute(builder: (_) => AreaScreen(draft: widget.draft)));
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Scaffold(
      appBar: AppBar(),
      body: FutureBuilder<List<Country>>(
        future: _future,
        builder: (context, snap) {
          if (snap.hasError) {
            return LoadError(
                error: snap.error!,
                onRetry: () => setState(() => _future = api.getCountries()));
          }
          if (!snap.hasData) {
            return const Center(child: CircularProgressIndicator());
          }
          final countries = snap.data!;
          return ListView(
            children: [
              const StepHeader(step: '2 / 4', title: '어느 나라로 가세요?'),
              ...countries.map((c) => Padding(
                    padding:
                        const EdgeInsets.symmetric(horizontal: 16, vertical: 6),
                    child: Card(
                      clipBehavior: Clip.antiAlias,
                      child: ListTile(
                        contentPadding: const EdgeInsets.symmetric(
                            horizontal: 20, vertical: 8),
                        leading: CircleAvatar(
                          backgroundColor: theme.colorScheme.primaryContainer,
                          child: Text(c.name.characters.first),
                        ),
                        title: Text(c.name,
                            style: theme.textTheme.titleMedium
                                ?.copyWith(fontWeight: FontWeight.bold)),
                        subtitle:
                            c.currency != null ? Text('통화 ${c.currency}') : null,
                        trailing: const Icon(Icons.chevron_right_rounded),
                        onTap: () => _select(c),
                      ),
                    ),
                  )),
            ],
          );
        },
      ),
    );
  }
}
